/*  Copyright (C) 2019-2024 Andreas Böhler

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
package nodomain.freeyourgadget.gadgetbridge.service.btle;

import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

public abstract class AbstractTransaction {
    private final String mName;
    private final long creationTimestamp = System.currentTimeMillis();

    public AbstractTransaction(String taskName) {
        this.mName = taskName;
    }

    public String getTaskName() {
        return mName;
    }

    protected String getCreationTime() {
        return DateTimeUtils.formatLocalTime(creationTimestamp);
    }

    public abstract int getActionCount();

    @Override
    public String toString() {
        return getCreationTime() + " " + getClass().getSimpleName() + " with "
                + getActionCount() + " actions for " + getTaskName();
    }

}
