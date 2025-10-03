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

import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Workout;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiWorkoutGbParser;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class GetWorkoutPaceRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(GetWorkoutPaceRequest.class);

    Workout.WorkoutCount.Response.WorkoutNumbers workoutNumbers;
    List<Workout.WorkoutCount.Response.WorkoutNumbers> remainder;
    short number;
    Long databaseId;

    public GetWorkoutPaceRequest(HuaweiSupportProvider support, Workout.WorkoutCount.Response.WorkoutNumbers workoutNumbers, List<Workout.WorkoutCount.Response.WorkoutNumbers> remainder, short number, Long databaseId) {
        super(support);

        this.serviceId = Workout.id;
        this.commandId = Workout.WorkoutPace.id;

        this.workoutNumbers = workoutNumbers;
        this.remainder = remainder;
        this.number = number;

        this.databaseId = databaseId;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new Workout.WorkoutPace.Request(paramsProvider, this.workoutNumbers.workoutNumber, this.number).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        if (!(receivedPacket instanceof Workout.WorkoutPace.Response))
            throw new ResponseTypeMismatchException(receivedPacket, Workout.WorkoutPace.Response.class);

        Workout.WorkoutPace.Response packet = (Workout.WorkoutPace.Response) receivedPacket;

        if (packet.error != null) {
            LOG.warn("Error {} occurred during workout pace sync. ignoring", packet.error);
            GB.toast("Error occurred during workout sync", Toast.LENGTH_LONG, GB.WARN);
            supportProvider.nextWorkoutSync(remainder, GetWorkoutPaceRequest.this.finalizeReq);
            return;
        }

        if (packet.workoutNumber != this.workoutNumbers.workoutNumber)
            throw new WorkoutParseException("Incorrect workout number!");

        if (packet.paceNumber != this.number)
            throw new WorkoutParseException("Incorrect pace number!");

        LOG.info("Workout {} pace {}:", this.workoutNumbers.workoutNumber, this.number);
        LOG.info("Workout  : {}", packet.workoutNumber);
        LOG.info("Pace     : {}", packet.paceNumber);
        LOG.info("Block num: {}", packet.blocks.size());
        LOG.info("Blocks   : {}", Arrays.toString(packet.blocks.toArray()));

        supportProvider.addWorkoutPaceData(this.databaseId, packet.blocks, packet.paceNumber);

        if (this.workoutNumbers.paceCount > this.number + 1) {
            GetWorkoutPaceRequest nextRequest = new GetWorkoutPaceRequest(
                    this.supportProvider,
                    this.workoutNumbers,
                    this.remainder,
                    (short) (this.number + 1),
                    this.databaseId
            );
            nextRequest.setFinalizeReq(this.finalizeReq);
            this.nextRequest(nextRequest);
        } else if (this.workoutNumbers.segmentsCount > 0) {
            GetWorkoutSwimSegmentsRequest nextRequest = new GetWorkoutSwimSegmentsRequest(
                    this.supportProvider,
                    this.workoutNumbers,
                    this.remainder,
                    (short) 0,
                    this.databaseId
            );
            nextRequest.setFinalizeReq(this.finalizeReq);
            this.nextRequest(nextRequest);
        } else if (this.workoutNumbers.spO2Count > 0) {
            GetWorkoutSpO2Request nextRequest = new GetWorkoutSpO2Request(
                    this.supportProvider,
                    this.workoutNumbers,
                    this.remainder,
                    (short) 0,
                    this.databaseId
            );
            nextRequest.setFinalizeReq(this.finalizeReq);
            this.nextRequest(nextRequest);
        } else if (this.workoutNumbers.sectionsCount > 0) {
            GetWorkoutSectionsRequest nextRequest = new GetWorkoutSectionsRequest(
                    this.supportProvider,
                    this.workoutNumbers,
                    this.remainder,
                    (short) 0,
                    this.databaseId
            );
            nextRequest.setFinalizeReq(this.finalizeReq);
            this.nextRequest(nextRequest);
        } else {
            new HuaweiWorkoutGbParser(getDevice(), getContext()).parseWorkout(this.databaseId);
            supportProvider.downloadWorkoutGpsFiles(this.workoutNumbers.workoutNumber, this.databaseId, () -> supportProvider.nextWorkoutSync(remainder, GetWorkoutPaceRequest.this.finalizeReq));
        }
    }
}
