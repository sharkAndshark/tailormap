/*
 * Copyright (C) 2019 B3Partners B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package nl.tailormap.viewer.audit.strategy;

import nl.tailormap.viewer.audit.AuditMessageObject;
import nl.tailormap.viewer.audit.Auditable;
import nl.tailormap.viewer.audit.LoggingServiceFactory;

/**
 *
 * @author Meine Toonen
 */
public class EditLogging implements LoggingStrategy{

    @Override
    public void log(Auditable ab, AuditMessageObject amo) {
        LoggingServiceFactory.getInstance().logMessage(amo.getUsername(), amo.getMessagesAsString());
    }
    
}
