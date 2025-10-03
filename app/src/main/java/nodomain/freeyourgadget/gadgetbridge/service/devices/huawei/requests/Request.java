/*  Copyright (C) 2024 Damien Gaignon, Martin.JM

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiCrypto;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.operations.OperationStatus;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

// Based on nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.requests.Request

/**
 * Add capacity to :
 *     - chain requests;
 *     - use data from a past request;
 *     - call a function after last request.
 */

public class Request {
    private static final Logger LOG = LoggerFactory.getLogger(Request.class);

    public static class RequestCreationException extends Exception {
        public RequestCreationException(String message) {
            super(message);
        }

        public RequestCreationException(HuaweiPacket.CryptoException e) {
            super(e);
        }

        public RequestCreationException(String message, Exception e) {
            super(message, e);
        }
    }

    public static class ResponseParseException extends Exception {
        public ResponseParseException(String message) {
            super(message);
        }

        public ResponseParseException(Exception e) {
            super(e);
        }

        public ResponseParseException(String message, Exception e) {
            super(message, e);
        }
    }

    public static class ResponseTypeMismatchException extends ResponseParseException {
        public ResponseTypeMismatchException(HuaweiPacket a, Class<?> b) {
            super("Response type mismatch, packet is of type " + a.getClass() + " but expected " + b);
        }

        public ResponseTypeMismatchException(HuaweiPacket a, Class<?> b, Class<?> c) {
            super("Response type mismatch, packet is of type " + a.getClass() + " but expected " + b + " or " + c);
        }
    }

    public static class WorkoutParseException extends ResponseParseException {
        public WorkoutParseException(String message) {
            super(message);
        }
    }

    protected OperationStatus operationStatus = OperationStatus.INITIAL;
    protected byte serviceId;
    protected byte commandId;
    protected HuaweiPacket receivedPacket = null;
    protected HuaweiSupportProvider supportProvider;
    protected HuaweiPacket.ParamsProvider paramsProvider;
    private nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder builderBr;
    private nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder builderLe;
    // Be able to autostart a request after this one
    protected Request nextRequest = null;
    protected boolean isSelfQueue = false;
    // Callback function to start after the request
    protected RequestCallback finalizeReq = null;
    // Stop chaining requests and clean support.inProgressRequests from these requests
    protected boolean stopChain = false;
    protected HuaweiCrypto huaweiCrypto = null;
    protected boolean addToResponse = true;

    private final Handler handler;
    private final Runnable timeoutRunner;
    private Integer timeout = null;

    public static class RequestCallback {
        protected HuaweiSupportProvider support = null;
        public RequestCallback() {}
        public RequestCallback(HuaweiSupportProvider supportProvider) {
            support = supportProvider;
        }
        public void call() {}
        public void call(Request request) {
            call(); // To keep everything working as it was as well
        }
        public void handleException(ResponseParseException e) {
            LOG.error("Callback request exception", e);
        }
        public void timeout(Request request) {
            request.handleNext();
        }
    }

    private Runnable getTimeoutRunnable() {
        return () -> {
            LOG.debug("Timeout on Service {} command {}", Integer.toHexString(this.serviceId & 0xff), Integer.toHexString(this.commandId & 0xff));
            if (finalizeReq != null)
                finalizeReq.timeout(this);
            else
                this.handleNext();
        };
    }

    public Request(HuaweiSupportProvider supportProvider, nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder builder) {
        this.supportProvider = supportProvider;
        this.paramsProvider = supportProvider.getParamsProvider();
        assert !supportProvider.isBLE();
        this.builderBr = builder;

        this.isSelfQueue = true;

        this.handler = new Handler(Looper.getMainLooper());
        this.timeoutRunner = getTimeoutRunnable();
    }

    public Request(HuaweiSupportProvider supportProvider, nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder builder) {
        this.supportProvider = supportProvider;
        this.paramsProvider = supportProvider.getParamsProvider();
        assert supportProvider.isBLE();
        this.builderLe = builder;

        this.isSelfQueue = true;

        this.handler = new Handler(Looper.getMainLooper());
        this.timeoutRunner = getTimeoutRunnable();
    }

    public Request(HuaweiSupportProvider supportProvider) {
        this.supportProvider = supportProvider;
        this.paramsProvider = supportProvider.getParamsProvider();

        if (!supportProvider.isBLE())
            this.builderBr = supportProvider.createBrTransactionBuilder(getName());
        else
            this.builderLe = supportProvider.createLeTransactionBuilder(getName());

        this.isSelfQueue = true;

        this.handler = new Handler(Looper.getMainLooper());
        this.timeoutRunner = getTimeoutRunnable();
    }

    protected boolean requestSupported() {
        return true;
    }

    public void doPerform() throws IOException {
        if (!requestSupported()) {
            this.handleNext();
            return;
        }

        if (this.addToResponse) {
            supportProvider.addInProgressRequest(this);
        }
        try {
            for (byte[] request : createRequest()) {
                int mtu = paramsProvider.getMtu();
                if (request.length >= mtu) {
                    ByteBuffer buffer = ByteBuffer.wrap(request);
                    byte[] data;
                    while (buffer.hasRemaining()) {
                        int delta = Math.min(mtu, buffer.remaining());
                        data = new byte[delta];
                        buffer.get(data, 0, delta);
                        builderWrite(data);
                    }
                } else {
                    builderWrite(request);
                }
            }
            builderWait(paramsProvider.getInterval()); // Need to wait a little to let some requests end correctly i.e. Battery Level on reconnection to not print correctly
            if (isSelfQueue) {
                performConnected();
            }
        } catch (RequestCreationException e) {
            // We cannot throw the RequestCreationException, so we throw an IOException
            throw new IOException("Request could not be created", e);
        }
    }

    protected List<byte[]> createRequest() throws RequestCreationException {
        return null;
    }

    protected void processResponse() throws ResponseParseException {}

    public void handleResponse() {
        // Stop timeout timer
        this.handler.removeCallbacks(this.timeoutRunner);

        try {
            this.receivedPacket.parseTlv();
        } catch (HuaweiPacket.ParseException e) {
            LOG.error("Parse TLV exception", e);
            if (finalizeReq != null)
                finalizeReq.handleException(new ResponseParseException("Parse TLV exception", e));
            return;
        }
        try {
            processResponse();
        } catch (ResponseParseException e) {
            if (finalizeReq != null)
                finalizeReq.handleException(e);
            return;
        }
        handleNext();
    }

    public void handleNext() {
        if (nextRequest != null && !stopChain) {
            try {
                nextRequest.doPerform();
            } catch (IOException e) {
                GB.toast(supportProvider.getContext(), "nextRequest failed", Toast.LENGTH_SHORT, GB.ERROR, e);
                LOG.error("Next request failed", e);
                if (finalizeReq != null)
                    finalizeReq.handleException(new ResponseParseException("Next request failed", e));
                return;
            }
        }
        if (nextRequest == null || stopChain) {
            operationStatus = OperationStatus.FINISHED;
            if (finalizeReq != null) {
                finalizeReq.call(this);
            }
        }
        nextRequest = null;
    }

    public void setSelfQueue() {
        isSelfQueue = true;
    }

    public Request nextRequest(Request req) {
        if (req != null) {
            nextRequest = req;
            nextRequest.setSelfQueue();
        }
        return this;
    }

    public void stopChain(Request req) {
        req.stopChain();
        Request next = req.nextRequest;
        if (next != null) {
            next.stopChain(next);
            supportProvider.removeInProgressRequests(next);
        }
    }

    public void stopChain() {
        stopChain = true;
    }

    /**
     * Handler for responses from the device
     * @param response The response packet
     * @return True if this request handles this response, false otherwise
     */
    public boolean handleResponse(HuaweiPacket response) {
        if (response.serviceId == serviceId && response.commandId == commandId) {
            receivedPacket = response;
            return true;
        }
        return false;
    }

    protected Context getContext() {
        return supportProvider.getContext();
    }

    protected GBDevice getDevice() {
        return supportProvider.getDevice();
    }

    public String getName() {
        Class<?> thisClass = getClass();
        while (thisClass.isAnonymousClass()) thisClass = thisClass.getSuperclass();
        return thisClass.getSimpleName();
    }

    public void setFinalizeReq(RequestCallback finalizeReq) {
        this.finalizeReq = finalizeReq;
    }

    private void builderWrite(byte[] data) {
        if (!this.supportProvider.isBLE()) {
            this.builderBr.write(data);
        } else {
            this.builderLe.write(HuaweiConstants.UUID_CHARACTERISTIC_HUAWEI_WRITE, data);
        }
    }

    private void builderWait(int millis) {
        if (!this.supportProvider.isBLE())
            this.builderBr.wait(millis);
        else
            this.builderLe.wait(millis);
    }

    private void performConnected() throws IOException {
        LOG.debug("Perform connected");

        // Start the timeout timer
        if (this.timeout != null)
            handler.postDelayed(this.timeoutRunner, this.timeout);

        if (!this.supportProvider.isBLE()) {
            builderBr.queueConnected();
        } else {
            builderLe.queueConnected();
        }
    }

    public boolean autoRemoveFromResponseHandler() {
        return true;
    }

    public void setupTimeoutUntilNext(int timeout) {
        this.timeout = timeout;
    }

    @Override
    protected void finalize() throws Throwable {
        // Stop timeout timer
        this.handler.removeCallbacks(this.timeoutRunner);

        super.finalize();
    }
}
