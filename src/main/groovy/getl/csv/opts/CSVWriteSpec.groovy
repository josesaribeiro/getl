/*
 GETL - based package in Groovy, which automates the work of loading and transforming data. His name is an acronym for "Groovy ETL".

 GETL is a set of libraries of pre-built classes and objects that can be used to solve problems unpacking,
 transform and load data into programs written in Groovy, or Java, as well as from any software that supports
 the work with Java classes.

 Copyright (C) EasyData Company LTD

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License and
 GNU Lesser General Public License along with this program.
 If not, see <http://www.gnu.org/licenses/>.
*/

package getl.csv.opts

import getl.lang.opts.BaseSpec
import getl.utils.BoolUtils
import groovy.transform.InheritConstructors
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType

/**
 * Options for reading CSV file
 * @author Alexsey Konstantinov
 *
 */
@InheritConstructors
class CSVWriteSpec extends BaseSpec {
    /** Check constraints while writing a file */
    Boolean getIsValid() { params.isValid as Boolean }
    /** Check constraints while writing a file */
    void setIsValid(Boolean value) { params.isValid = value }

    /** Batch size packet */
    Long getBatchSize() { params.batchSize as Long }
    /** Batch size packet */
    void setBatchSize(Long value) { params.batchSize = value }

    /**
     * Run after save batch records
     * <br>Closure parameters: Long numberOfBatch
     */
    Closure getOnSaveBatch() { params.onSaveBatch as Closure }
    /**
     * Run after save batch records
     * <br>Closure parameters: Long numberOfBatch
     */
    void setOnSaveBatch(Closure value) { params.onSaveBatch = value }
    /**
     * Run after save batch records
     * <br>Closure parameters: Long numberOfBatch
     */
    void saveBatch(@ClosureParams(value = SimpleType, options = ['long']) Closure value) {
        setOnSaveBatch(value)
    }

    /** Maximum size of the portion of the recorded file (use 0 or null for no size limit) */
    Long getSplitSize() { params.splitSize as Long }
    /** Maximum size of the portion of the recorded file (use 0 or null for no size limit) */
    void setSplitSize(Long value) { params.splitSize = value }

    /**
     * Checking row for need to write current and next rows to the new file
     * <br>Closure parameters: Map row
     */
    Closure<Boolean> getOnSplitFile() { params.onSplitFile as Closure<Boolean> }
    /**
     * Checking row for need to write current and next rows to the new file
     * <br>Closure parameters: Map row
     */
    void setOnSplitFile(Closure<Boolean> value) {
        params.onSplitFile = value
    }
    /**
     * Checking row for need to write current and next rows to the new file
     * <br>Closure parameters: Map row
     */
    void splitFile(@ClosureParams(value = SimpleType, options = ['java.util.HashMap'])
                           Closure<Boolean> value) {
        setOnSplitFile(value)
    }

    /** Parts of files are available immediately after writing */
    Boolean getAvaibleAfterWrite() { params.avaibleAfterWrite as Boolean }
    /** Parts of files are available immediately after writing */
    void setAvaibleAfterWrite(Boolean value) { params.avaibleAfterWrite = value }
}