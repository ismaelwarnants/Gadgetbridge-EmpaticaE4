/*  Copyright (C) 2024 Martin.JM

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
package nodomain.freeyourgadget.gadgetbridge.database.schema;

import android.database.sqlite.SQLiteDatabase;

import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.database.DBUpdateScript;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutDataSampleDao;

public class GadgetbridgeUpdate_72 implements DBUpdateScript {
    @Override
    public void upgradeSchema(final SQLiteDatabase db) {
        if (!DBHelper.existsColumn(HuaweiWorkoutDataSampleDao.TABLENAME, HuaweiWorkoutDataSampleDao.Properties.Calories.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiWorkoutDataSampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiWorkoutDataSampleDao.Properties.Calories.columnName + "\" INTEGER NOT NULL DEFAULT -1";
            db.execSQL(statement);
        }
        if (!DBHelper.existsColumn(HuaweiWorkoutDataSampleDao.TABLENAME, HuaweiWorkoutDataSampleDao.Properties.CyclingPower.columnName, db)) {
            final String statement = "ALTER TABLE " + HuaweiWorkoutDataSampleDao.TABLENAME + " ADD COLUMN \""
                    + HuaweiWorkoutDataSampleDao.Properties.CyclingPower.columnName + "\" INTEGER NOT NULL DEFAULT -1";
            db.execSQL(statement);
        }
    }

    @Override
    public void downgradeSchema(final SQLiteDatabase db) {
    }
}
