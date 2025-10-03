/*  Copyright (C) 2024 Damien Gaignon

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Notifications;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Notifications.NotificationConstraints;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class GetNotificationConstraintsRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(GetNotificationConstraintsRequest.class);

    public GetNotificationConstraintsRequest(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = Notifications.id;
        this.commandId = NotificationConstraints.id;
    }

    @Override
    protected boolean requestSupported() {
        return supportProvider.getHuaweiCoordinator().supportsNotificationAlert() && supportProvider.getProtocolVersion() == 2
                && supportProvider.getCoordinator().getDeviceType() != DeviceType.HUAWEIBANDAW70
                && supportProvider.getCoordinator().getDeviceType() != DeviceType.HONORWATCH4; // Bit of a workaround, there is probably some capabilities this actually depends on
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new NotificationConstraints.Request(paramsProvider).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        LOG.debug("handle Get Notification Constraint");

        if (!(receivedPacket instanceof NotificationConstraints.Response))
            throw new ResponseTypeMismatchException(receivedPacket, NotificationConstraints.Response.class);

        supportProvider.getHuaweiCoordinator().saveNotificationConstraints(((NotificationConstraints.Response) receivedPacket).constraints);
    }
}
