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

package getl.transform

import getl.data.*
import getl.driver.VirtualDatasetDriver
import getl.exception.ExceptionGETL
import getl.utils.GenerationUtils
import groovy.transform.InheritConstructors

/**
 * Aggregation driver class
 * @author Alexsey Konstantinov
 *
 */
@InheritConstructors
class AggregatorDatasetDriver extends VirtualDatasetDriver {
	
	private static List<String> getFieldByGroup(Dataset dataset) {
		def res = dataset.params.fieldByGroup as List<String>
		if (res == null) throw new ExceptionGETL("Required parameter \"fieldByGroup\" in dataset")
		res
	}
	
	private static Map<String, Map> getFieldCalc(Dataset dataset) {
		def res = dataset.params.fieldCalc as Map<String, Map>
		if (res == null) throw new ExceptionGETL("Required parameter \"fieldCalc\" in dataset")
		res
	}
	
	@Override

	void openWrite(Dataset dataset, Map params, Closure prepareCode) {
		Dataset ds = getDestinition(dataset)
		def fieldByGroup = getFieldByGroup(dataset)
		def fieldCalc = getFieldCalc(dataset)
		def algorithm = dataset.params.algorithm
		if (algorithm == null) throw new ExceptionGETL("Required parameter \"algorithm\" in dataset")
		algorithm = (algorithm as String).toUpperCase()
		if (!(algorithm in ["HASH", "TREE"])) throw new ExceptionGETL("Unknown algorithm \"${algorithm}\"")
		Closure aggregateCode = generateAggrCode(fieldByGroup, fieldCalc)
		Map filter = [:]
		fieldCalc.each { name, value ->
			if (value.filter != null) {
				filter.put(name.toLowerCase(), value.filter)
			}
		}

		if (prepareCode != null) prepareCode.call(dataset.field)
		
		ds.openWrite(params)
		
		if (algorithm == "HASH") {
			LinkedHashMap data = new LinkedHashMap()
			dataset.params.aggregator_data = data
		}
		else {
			TreeMap data = new TreeMap()
			dataset.params.aggregator_data = data
		}
		
		dataset.params.aggregator_code = aggregateCode
		dataset.params.aggregator_filter = filter
	}

	@Override

	void write(Dataset dataset, Map row) {
		def data = dataset.params.aggregator_data as Map
		def aggregateCode = dataset.params.aggregator_code as Closure
		def filter = dataset.params.aggregator_filter as Map
		aggregateCode.call(row, data, filter)
	}

	@Override

	void doneWrite(Dataset dataset) {
		Dataset ds = getDestinition(dataset)
		def data = dataset.params.aggregator_data as Map<Object, Map>
		data.each { key, value ->
			ds.write(value)
		}
		ds.doneWrite()
	}

	@Override

	void closeWrite(Dataset dataset) {
		Dataset ds = getDestinition(dataset)
		ds.closeWrite()
		
		dataset.params.aggregator_data = null
		dataset.params.remove("aggregator_data")
		dataset.params.remove("aggregator_code")
	}
	
	private static Closure generateAggrCode(List<String> fieldByGroup, Map<String, Map> fieldCalc) {
		StringBuilder sb = new StringBuilder()
		sb << """{ Map row, Map data, Map filter ->
	List<String> key = []
	Map keyM = [:]
"""
		int num = 0
		if (!fieldByGroup.isEmpty()) {
			fieldByGroup.each { String name ->
				num++
				name = name.toLowerCase()
				sb << """
	String k_${num} = (row.'${name}' != null)?row.'${name}'.toString().toLowerCase():"NULL"
	key << k_${num}
	keyM.'${name}' = row.'${name}'
"""
			}
		
			sb << "	String keyS = key.join(\"\\t\")"
		}
		else {
			sb << "	String keyS = \"*ALL*\""
		}
			
		sb << """
	Map value = data.get(keyS)
	if (value == null) {
		value = keyM
		data.put(keyS, value)
	}
"""
		num = 0
		fieldCalc.each { String name, Map mp ->
			name = name.toLowerCase().replace("'", "\\'")
			String method = (mp.method != null)?(mp.method as String).toUpperCase():"SUM"
			def filter = mp.filter as Closure
			def source = (mp.fieldName as String)?.replace("'", "\\'")
			if (source == null && method != "COUNT") throw new ExceptionGETL("Required fieldName in parameters by field \"${name}\"")
			
			if (filter != null) {
				sb << """
	Closure valid = filter."${name}"
	if (valid(row)) {
"""
			}
			 
			switch (method) {
				case "COUNT":
					sb << """
	if (value.'${name}' != null) value.'${name}' = value.'${name}' + 1 else value.'${name}' = 1  
"""
					break

				case "SUM":
					sb << """
	if (row.'${source}' != null) {
		if (value.'${name}' != null) 
			value.'${name}' = value.'${name}' + row.'${source}'
		else
			value.'${name}' = row.'${source}'
	}
"""
					break

				case "MIN":
					sb << """
	if (row.'${source}' != null && ((value.'${name}' == null || row.'${source}' < value.'${name}'))) value.'${name}' = row.'${source}'
"""
					break

				case "MAX":
					sb << """
	if (row.'${source}' != null && ((value.'${name}' == null || row.'${source}' > value.'${name}'))) value.'${name}' = row.'${source}'
"""
					break

				default:
					throw new ExceptionGETL("Not supported method ${method}")
			}
			
			if (filter != null) sb << "}\n"
		}
		sb << "}"
		
		Closure result = GenerationUtils.EvalGroovyClosure(sb.toString())
		
		result
	}
}
