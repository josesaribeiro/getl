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

package getl.csv

import getl.csv.opts.CSVReadSpec
import getl.csv.opts.CSVWriteSpec
import getl.jdbc.opts.ReadSpec
import getl.lang.Getl
import getl.lang.opts.BaseSpec
import getl.vertica.opts.VerticaReadSpec
import groovy.transform.InheritConstructors
import getl.data.Connection
import getl.data.FileDataset
import getl.exception.ExceptionGETL
import getl.utils.*

/**
 * CSV Dataset class
 * @author Alexsey Konstantinov
 *
 */
@InheritConstructors
class CSVDataset extends FileDataset {
	static enum QuoteMode {ALWAYS, NORMAL, COLUMN}

	/**
	 * Quote delimiter string	
	 */
	String getQuoteStr () { ListUtils.NotNullValue([params.quoteStr, csvConnection()?.quoteStr, '"']) }
	/**
	 * Quote delimiter string
	 */
	void setQuoteStr (String value) { params.quoteStr = value }
	
	/**
	 * Field delimiter
	 */
	String getFieldDelimiter () { ListUtils.NotNullValue([params.fieldDelimiter, csvConnection()?.fieldDelimiter, ',']) }
	/**
	 * Field delimiter
	 */
	void setFieldDelimiter (String value) { params.fieldDelimiter = value }
	
	/**
	 * Row delimiter
	 */
	String getRowDelimiter () { ListUtils.NotNullValue([params.rowDelimiter, csvConnection()?.rowDelimiter,'\n']) }
	/**
	 * Row delimiter
	 */
	void setRowDelimiter (String value) { params.rowDelimiter = value }
	
	/**
	 * File has header of fields name
	 */
	boolean getHeader () { BoolUtils.IsValue([params.header, csvConnection()?.header], true) }
	/**
	 * File has header of fields name
	 */
	void setHeader (boolean value) { params.header = value }
	
	/**
	 * Ignore header field name
	 */
	boolean getIgnoreHeader () { BoolUtils.IsValue([params.ignoreHeader, csvConnection()?.ignoreHeader], true) }
	/**
	 * Ignore header field name
	 */
	void setIgnoreHeader (boolean value) { params.ignoreHeader = value }
	
	/**
	 * Required format values for output to file 
	 */
	boolean getFormatOutput () { BoolUtils.IsValue([params.formatOutput, csvConnection()?.formatOutput], true) }
	/**
	 * Required format values for output to file
	 */
	void setFormatOutput (boolean value) { params.formatOutput = value }
	
	/**
	 * Convert NULL to value
	 */
	String getNullAsValue () { ListUtils.NotNullValue([params.nullAsValue, csvConnection()?.nullAsValue]) }
	/**
	 * Convert NULL to value
	 */
	void setNullAsValue (String value) { params.nullAsValue = value }

	/**
	 * Required convert string to escape value 	
	 */
	boolean getEscaped () { BoolUtils.IsValue([params.escaped, csvConnection()?.escaped], false) }
	/**
	 * Required convert string to escape value
	 */
	void setEscaped (boolean value) { params.escaped = value }
	
	/**
	 * Convert line feed to custom escape char 
	 */
	String getEscapeProcessLineChar () { ListUtils.NotNullValue([params.escapeProcessLineChar, csvConnection()?.escapeProcessLineChar]) }
	/**
	 * Convert line feed to custom escape char
	 */
	void setEscapeProcessLineChar (String value) { params.escapeProcessLineChar = value }
	
	/**
	 * Mode of quote value 
	 */
	QuoteMode getQuoteMode () { ListUtils.NotNullValue([params.quoteMode, csvConnection()?.quoteMode, QuoteMode.NORMAL]) as QuoteMode }
	/**
	 * Mode of quote value
	 */
	void setQuoteMode (QuoteMode value) { params.quoteMode = value }
	
	/**
	 * Decimal separator for number fields
	 */
	String getDecimalSeparator () { ListUtils.NotNullValue([params.decimalSeparator, csvConnection()?.decimalSeparator, '.']) }
	/**
	 * Decimal separator for number fields
	 */
	void setDecimalSeparator (String value) { params.decimalSeparator = value }
	
	/**
	 * Format for date fields
	 */
	String getFormatDate () { ListUtils.NotNullValue([params.formatDate, csvConnection()?.formatDate]) }
	/**
	 * Format for date fields
	 */
	void setFormatDate (String value) { params.formatDate = value }
	
	/**
	 * Format for time fields
	 */
	String getFormatTime () { ListUtils.NotNullValue([params.formatTime, csvConnection()?.formatTime]) }
	/**
	 * Format for time fields
	 */
	void setFormatTime (String value) { params.formatTime = value }
	
	/**
	 * Format for datetime fields
	 */
	String getFormatDateTime () { ListUtils.NotNullValue([params.formatDateTime, csvConnection()?.formatDateTime]) }
	/**
	 * Format for datetime fields
	 */
	void setFormatDateTime (String value) { params.formatDateTime = value }

	/** OS locale for parsing date-time fields
	 * <br>P.S. You can set locale for separately field in Field.extended.locale
	 */
	String getLocale() { ListUtils.NotNullValue([params.locale, csvConnection()?.locale]) }
	/** OS locale for parsing date-time fields
	 * <br>P.S. You can set locale for separately field in Field.extended.locale
	 */
	void setLocale(String value) { params.locale = value }
		
	/**
	 * Length of the recorded file
	 */
	Long getCountWriteCharacters() { params.countWriteCharacters }
	
	/**
	 * the number of recorded files
	 */
	Integer getCountWritePortions() { params.countWritePortions }
	
	/**
	 * The number of read files
	 */
	Integer getCountReadPortions() { params.countReadPortions }
	
	@Override
	void setConnection(Connection value) {
		if (value != null && !(value instanceof CSVConnection))
			throw new ExceptionGETL('Сonnection to CSVConnection class is allowed!')

		super.setConnection(value)
	}
	
	@Override
	List<String> inheriteConnectionParams () {
		super.inheriteConnectionParams() + 
				['quoteStr', 'fieldDelimiter', 'rowDelimiter', 'header', 
					'escaped', 'decimalSeparator', 'formatDate', 'formatTime', 'formatDateTime', 'ignoreHeader', 
					'escapeProcessLineChar', 'nullAsValue']
	}

	/**
	 * Current CSV connection
	 */
	CSVConnection csvConnection() { connection as CSVConnection}
	
	/**
	 * Convert from source CSV file with encoding code page and escaped
	 */
	long prepareCSVForBulk (CSVDataset source, Map encodeTable, Closure code) {
		CSVDriver drv = connection.driver as CSVDriver
		
		drv.prepareCSVForBulk(this, source, encodeTable, code)
	}
	
	/**
	 * Convert from source CSV file with encoding code page and escaped
	 */
	long prepareCSVForBulk (CSVDataset source, Map encodeTable) {
		prepareCSVForBulk(source, encodeTable, null)
	}
	
	/**
	 * Convert from source CSV file with encoding code page and escaped
	 */
	long prepareCSVForBulk(CSVDataset source) {
		prepareCSVForBulk(source, null, null)
	}
	
	/**
	 * Convert from source CSV file with encoding code page and escaped
	 */
	long prepareCSVForBulk (CSVDataset source, Closure code) {
		prepareCSVForBulk(source, null, code)
	}
	
	/**
	 * Decoding prepare for bulk load file
	 */
	long decodeBulkCSV (CSVDataset source) {
		CSVDriver drv = connection.driver as CSVDriver
		drv.decodeBulkCSV(this, source)
	}
	
	/**
	 * Count rows of file
	 */
	long readRowCount (Map params) {
		long res = 0
		eachRow((params?:[:]) + [readAsText: true]) {
			res++
		}
		
		res
	}
	
	/**
	 * File lines count 
	 */
	long readLinesCount () {
		CSVDriver drv = connection.driver as CSVDriver
		
		drv.readLinesCount(this)
	}

	/**
	 * Read file options
	 */
	CSVReadSpec readOpts(@DelegatesTo(CSVReadSpec) Closure cl = null) {
		def ownerObject = sysParams.dslOwnerObject?:this
		def thisObject = sysParams.dslThisObject?:BaseSpec.DetectClosureDelegate(cl)
		def parent = new CSVReadSpec(ownerObject, thisObject, true, readDirective)
		parent.runClosure(cl)

		return parent
	}

	/**
	 * Write file options
	 */
	CSVWriteSpec writeOpts(@DelegatesTo(CSVWriteSpec) Closure cl = null) {
		def ownerObject = sysParams.dslOwnerObject?:this
		def thisObject = sysParams.dslThisObject?:BaseSpec.DetectClosureDelegate(cl)
		def parent = new CSVWriteSpec(ownerObject, thisObject, true, writeDirective)
		parent.runClosure(cl)

		return parent
	}
}