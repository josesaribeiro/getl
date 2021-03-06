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

package getl.utils

import getl.data.Field
import getl.utils.sub.ClosureScript
import groovy.json.JsonBuilder
import groovy.json.JsonGenerator
import groovy.json.JsonSlurper
import getl.exception.ExceptionGETL
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import org.apache.groovy.json.internal.LazyMap

/**
 * Map library functions class
 * @author Alexsey Konstantinov
 *
 */
@CompileStatic
class MapUtils {
	/**
	 * Copy all map items as new objects with Json builder
	 * @param map
	 * @return
	 */
	static Map DeepCopy(Map map) {
		if (map == null) return null
		
		Map res
		
		def str = ToJson(map)
		
		def json = new JsonSlurper()
		res = (Map)(json.parseText(str))

		return res
	}

	/**
	 * Set keys of map to lower case
	 * @param m
	 * @return
	 */
	static Map MapToLower(Map<String, Object> m) {
		if (m == null) return null
		
		def r = [:]
		m.each { String k, v ->
			r.put(k.toLowerCase(), v)
		}

		return r
	}
	
	/**
	 * Clean maps with list of key
	 * @param m
	 * @param keys
	 * @return
	 */
	static Map CleanMap (Map m, List<String> keys) {
		if (m == null) return null
		
		Map result = [:]
		result.putAll(m)
		keys.each {
			result.remove(it)
		}

		return result
	}
	
	/**
	 * Remove keys from map
	 * @param m
	 * @param keys
	 */
	static void RemoveKeys (Map m, List<String> keys) {
		if (m == null) return
		
		keys.each {
			m.remove(it)
		}
	}
	
	/**
	 * Get only level naming map values 
	 * @param map
	 * @param level
	 * @return
	 */
	static Map GetLevel (Map map, String level) {
		if (map == null) return null
		
		Map result = [:]
		int lengthLevel = level.length()
		map.each { k, v ->
			def s = k.toString()
			if (level.length() < s.length() && level == s.toLowerCase().substring(0, lengthLevel)) {
				result.put(s.substring(lengthLevel), v)
			}
			
		}
		return result
	}
	
	/**
	 * Clone map
	 * @param map
	 * @return
	 */
	static Map Copy(Map map) {
		if (map == null) return null
		
		Map m = [:]
		m.putAll(map)
		
		return m
	}
	
	/**
	 * Clone map exclude specified keys
	 * @param map
	 * @param excludeKeys
	 * @return
	 */
	static Map Copy(Map map, List<String> excludeKeys) {
		if (map == null) return null
		
		Map m = [:]
		m.putAll(map)
		if (!excludeKeys.isEmpty()) m = CleanMap(m, excludeKeys)
		
		return m
	}
	
	/**
	 * Clone map include specified keys
	 * @param map
	 * @param includeKeys
	 * @return
	 */
	static Map CopyOnly(Map <String, Object> map, List<String> includeKeys) {
		if (map == null) return null
		
		Map res = [:]
		def keys = ListUtils.ToLowerCase(includeKeys)
		
		map.each { String k, v ->
			if (keys.find { it == k.toLowerCase()} ) res.put(k, v)
		}
		
		return res
	}
	
	/**
	 * Analize and return unknown keys in map
	 * @param map
	 * @param definedKey
	 * @param ignoreComments
	 * @return
	 */
	static List<String> Unknown(Map<String, Object> map, List<String> definedKey, boolean ignoreComments = false) {
		if (map == null) map = [:] as Map<String, Object>
		
		List<String> res = []
		(map as Map<String, Object>).each { k, v ->
			if (ignoreComments && k.substring(0, 1) == "_") return
			def o = definedKey.find { d ->
				if (d.matches(".*[*]")) {
					def s = d.substring(0, d.length() - 1)
					return k.length() > s.length() && k.substring(0, s.length()) == s
				} 
				k == d
			}
			if (o == null) res << k
		}

		return res
	}

	/**
	 * Map key verification
	 * @param map checked map
	 * @param definedKey list of allowed keys
	 * @param ignoreComments ignore keys whose name begins with an underscore
	 */
	static void CheckKeys(Map<String, Object> map, List<String> definedKey, boolean ignoreComments = false) {
		def u = Unknown(map, definedKey, ignoreComments)
		if (u.size() != 0)
			throw new ExceptionGETL("Unknown map keys detected: $u!")
	}
	
	/**
	 * Find value of name section from Map
	 * @param content
	 * @param section
	 * @return
	 */
	static Map FindSection (Map content, String section) {
		if (content == null) return null
		
		String[] sections = section.split("[.]")
		Map cur = content
		sections.each {
			if (cur != null) {
                if (it == '*') {
                    def next = cur.get(cur.keySet().toArray()[0])
                    if (next instanceof Map) cur = next else cur = null
                }
                else if (!cur.containsKey(it)) {
					cur = null
				}
				else {
                    def next = (cur.get(it))
                    if (next instanceof Map) cur = next else cur = null
				}
			}
		}

		return (cur != content)?cur:null
	}
	
	/**
	 * Validate existing name section from Map
	 * @param content
	 * @param section
	 * @return
	 */
	static boolean ContainsSection (Map content, String section) {
		return (FindSection(content, section) != null)
	}
	
	/**
	 * Set value to name section from Map
	 * @param content
	 * @param name
	 * @param value
	 */
	static void SetValue(Map<String, Object> content, String name, value) {
		if (content == null) return
		if (name == null) return
		
		String[] sections = name.split("[.]")
		Map <String, Object> cur = content
		for (int i = 0; i < sections.length - 1; i++) {
			String s = sections[i]
			if (cur.get(s) == null) {
				def m = [:] as Map <String, Object>
				cur.put(s, m)
				cur = m
			}
			else {
				cur = (Map<String, Object>)(cur.get(s))
			}
		}
		cur.put(sections[sections.length - 1], value)
	}
	
	/**
	 * Merge map
	 * @param source
	 * @param added
     * @param mergeList
	 */
	static void MergeMap (Map<String, Object> source, Map<String, Object> added, boolean existUpdate = true, boolean mergeList = false) {
		if (source == null || added == null) return
		added.each { String key, value ->
			MergeMapChildren(source, value, key, existUpdate, mergeList)
		}
	}

    /**
     * Merge children map node
     * @param source
     * @param added
     * @param key
     * @param mergeList
     */
	private static void MergeMapChildren (Map source, def added, String key, boolean existUpdate, boolean mergeList) {
		if (!(added instanceof Map)) {
			if (mergeList && added instanceof List) {
                List origList = source.get(key) as List
                if (origList != null) {
                    List newList = origList + (added as List)
                    source.put(key, newList.unique())
                }
                else {
                    source.put(key, added)
                }
            }
            else {
                if (!existUpdate && source.containsKey(key)) return
                source.put(key, added)
            }

			return
		}
		def c = added as Map<String, Object>
        def subsource = source.get(key) as Map<String, Object>
        if (subsource == null) {
            subsource = [:] as Map<String, Object>
            source.put(key, subsource)
        }
		c.each { String subkey, value ->
			MergeMapChildren(subsource, value, subkey, existUpdate, mergeList)
		}
	}

	/**
	 * Process arguments to Map
	 * @param args
	 * @return
	 */
	static Map<String, Object> ProcessArguments (def args) {
		List<String> la
		if (args instanceof List) {
			la = (List<String>)args
		}
		else if (args instanceof String[]) {
			la = (args as String[]).collect() as List<String>
		}
		else if (args instanceof String) {
			la = [args as String]
		}
		else {
			throw new ExceptionGETL("Invalid arguments for processing")
		}
		Map<String, Object> res = [:]
		if (args != null) {
			for (int i = 0; i < la.size(); i++) {
				def str = la[i]
				if (str.trim().length() == 0) continue
				def le = str.indexOf('=')
				String name
				String value
				if (le != -1) {
					name = str.substring(0, le)
					def c = name.indexOf('.')
					if (c == -1) {
						 name = name.toLowerCase().trim()
					}
					else {
						name = name.substring(0, c + 1).toLowerCase() + name.substring(c + 1).trim()
					}
					value = str.substring(le + 1)
				}
				else {
					name = la[i].trim()
					if (i < la.size() - 1) {
						if (la[i + 1].substring(0, 1) != '-') {
							i++
							value = la[i]
						}
					} 
				}

				if (name.substring(0, 1) == '-') name = name.substring(1).trim()
				if (value != null) {
					value = value.trim()
					def p = value =~ /"(.+)"/
					if (p.size() == 1) {
						List l = (List)p[0]
						value = l[1]
					}
				}
				
				SetValue(res, name, value)
			}
		}

		return res
	}
	
	/**
	 * Convert map to JSon text
	 * @param value
	 * @return
	 */
	static String ToJson (Map value) {
		if (value == null) return null

		def gen = new JsonGenerator.Options().timezone(TimeZone.default.getID())
		def build = new JsonBuilder(gen.build())
		build.call(value)

		return build.toPrettyString()
	}
	
	/**
	 * Evaluate string map value from variables
	 * @param value
	 * @param vars
	 * @return
	 */
	static Map EvalMacroValues(Map value, Map vars) {
		if (value == null) return null
		
		def res = [:]
		value.each { k, v ->
			if (v instanceof String || v instanceof GString) {
				def val = v.toString().replace("\\", "\\\\").replace('"""', '\\"\\"\\"').replace('${', '\u0001{').replace('$', '\\$').replace('\u0001{', '${')

				if (val.trim() != '"') res.put(k, GenerationUtils.EvalGroovyScript('"""' + val + '"""', vars, true)) else res.put(k, val)
			}
			else if (v instanceof Map) {
				res.put(k, EvalMacroValues(v as Map, vars))
			}
			else if (v instanceof List) {
				res.put(k, ListUtils.EvalMacroValues(v as List, vars))
			}
			else {
				res.put(k, v)
			}
		}

		return res
	}

    /**
     * Convert lazy map to hash map
     * @param value
     * @return
     */
	static HashMap Lazy2HashMap(Map data) {
        def res = [:] as HashMap
        data.each { key, value ->
			if (value instanceof LazyMap)
				value = Lazy2HashMap((Map)value)
			else if (value instanceof List)
				value = Lazy2HashMapList(value)

            res.put(key, value)
        }

        return res
	}

	static private List Lazy2HashMapList(List data) {
		def res = []
		data.each { value ->
			if (value instanceof LazyMap)
				value = Lazy2HashMap((Map)value)
			else if (value instanceof List)
				value = Lazy2HashMapList(value)

			res << value
		}

		return res
	}

	static void FindKeys(Map<String, Object> map, String expression,
						 @ClosureParams(value = SimpleType, options = ['java.util.HashMap', 'java.lang.String', 'java.lang.Object'])
								 Closure closure) {
		if (map == null || map.isEmpty() || expression == null || expression.length() == 0) return

		def keys = expression.split('[.]')
		FindKeysProcess(map, keys, 0, closure)
	}

	private static void FindKeysProcess(Map<String, Object> map, String[] keys, int cur, Closure closure) {
		def key = keys[cur]
		def len = keys.length - 1
		if (key != '*') {
			if (!map.containsKey(key)) return
			def item = map.get(key)
			if (cur == len) {
				closure.call(map, key, item)
			}
			else if (item instanceof Map) {
				FindKeysProcess(item as Map<String, Object>, keys, cur + 1, closure)
			}
			else if (item instanceof List) {
				FindKeysProcess(item as List, keys, cur + 1, closure)
			}
		}
		else {
			map.each { String name, item ->
				if (cur == len) {
					closure.call(map, name, item)
				}
				else if (item instanceof Map) {
					FindKeysProcess(item as Map<String, Object>, keys, cur + 1, closure)
				}
				else if (item instanceof List) {
					FindKeysProcess(item as List, keys, cur + 1, closure)
				}
			}
		}
	}

	private static void FindKeysProcess(List list, String[] keys, int cur, Closure closure) {
		def key = keys[cur]
		if (key != '*') throw new ExceptionGETL('Invalid format mask for list item!')
		list.each { item ->
			if (item instanceof Map) {
				FindKeysProcess(item as Map<String, Object>, keys, cur + 1, closure)
			}
			else if (item instanceof List) {
				FindKeysProcess(item as List, keys, cur + 1, closure)
			}
		}
	}

	/**
	 * Convert map structure to url parameters
	 * @param m
	 * @return
	 */
	static String MapToUrlParams(Map<String, Object> m) {
        List l = []
        m.each { String k, v ->
            l << "$k=${v.toString()}"
        }

        return l.join('&')
    }

    @CompileDynamic
	@SuppressWarnings("GrUnresolvedAccess")
	static Map<String, Object> Xml2Map(def node) {
		def rootName = node.name().localPart as String
        def rootMap = Xml2MapAttrs(node)
        def res = [:] as Map<String, Object>
        res.put("$rootName".toString(), rootMap)
        return res
    }

    /**
     * Convert XML node attributes to Map object
     * @param node
     * @return
     */
	@CompileDynamic
	@SuppressWarnings("GrUnresolvedAccess")
	private static Map<String, Object> Xml2MapAttrs(def node) {
		def res = [:] as Map<String, Object>

		if (node.attributes().size() > 0)
			try {
				res.putAll(node.attributes() as Map<String, Object>)
			}
			catch (Exception e) {
				Logs.Severe("Error parse attrubutes for xml node: $node")
				throw e
			}

		if (node.children().size() > 0)
			res.putAll(XmlChildren2Map(node.children()))

		return res
	}

    /**
     * Convert XML node children to Map object
     * @param childrens
     * @return
     */
	@CompileDynamic
	private static Map<String, Object> XmlChildren2Map(def childrens) {
		def res = [:] as Map<String, Object>

		childrens.each { def node ->
			try {
				def name = node.name().localPart as String

				if (node.children().size() == 1 && node.children().get(0) instanceof String) {
					res.put(name, node.children().get(0))
					return
				}

				if (node.attributes().name == null &&
						(node.children().size() == 0 || node.children().get(0).attributes().name == null)) {
					List<Map> values
					if (!res.containsKey(name)) {
						values = [] as List<Map>
						res."$name" = values
					} else {
						values = res."$name" as List<Map>
					}

					def nodeMap = Xml2MapAttrs(node)
					values << nodeMap
				}
				else {
					Map values
					if (!res.containsKey(name)) {
						values = [:] as Map
						res."$name" = values
					} else {
						values = res."$name" as Map
					}

					def nodeMap = Xml2MapAttrs(node)
					def nodeName = nodeMap.name
					if (nodeName != null) {
						RemoveKeys(nodeMap, ['name'])
						values.put(nodeName, nodeMap)
					}
					else {
						values.putAll(nodeMap)
					}
				}
			}
			catch (Exception e) {
				Logs.Severe("Error parse xml node: $node")
				throw e
			}
		}

		return res
	}

    /**
     * Process XSD content with reader and convert to Map
     * @param xsdName
     * @param read
     * @return
     */
    @CompileDynamic
	static Map<String, Object> Xsd2Map(String xsdName, List<String> exclude, Closure reader) {
        def data = reader.call(xsdName)
        def map = Xml2Map(data)
        if (exclude == null) exclude = [] as List<String>
        exclude << xsdName
		(map.schema as Map).include?.each { Map includeXsd ->
            String schemaLocation = includeXsd.schemaLocation
            if (schemaLocation == null)
                throw new ExceptionGETL("Invalid XSD include section: $includeXsd!")

            if (schemaLocation in exclude) return

            def includeMap = Xsd2Map(schemaLocation, exclude, reader)
			MergeMap(map, includeMap, false, true)

            exclude << schemaLocation
        }

        return map
    }

    /**
     * Process XSD file with resource and convert to Map
     * @param path - path from resource
     * @param fileName - xsd file name
     * @return
     */
    @CompileDynamic
	static Map<String, Object> XsdFromResource(String path, String fileName) {
        return XsdFromResource(ClassLoader.systemClassLoader, path, fileName)
    }

	@CompileDynamic
	/**
	 * Process XSD file with resource and convert to Map
	 * @classLoader - class loader for resource
	 * @param path - path from resource
	 * @param fileName - xsd file name
	 * @return
	 */
	static Map<String, Object> XsdFromResource(ClassLoader classLoader, String path, String fileName) {
		def xml = new XmlParser()

		def reader = { xsdFileName ->
			def xsdPath = "$path/$xsdFileName".toString()
			Logs.Config("Read resource xsd file \"$xsdPath\"")
			def input = classLoader.getResourceAsStream(xsdPath)
			if (input == null)
				throw new ExceptionGETL("Xsd resource \"$xsdPath\" not found!")

			return xml.parse(input)
		}

		return Xsd2Map(fileName, null, reader)
	}

	@SuppressWarnings("GrUnresolvedAccess")
	@CompileDynamic
	static List<Field> XsdMap2Fields(Map<String, Object> map, String objectTypeName) {
		if (map == null) 
            throw new ExceptionGETL('Parameter "map" required!')

        if (map.isEmpty()) 
            throw new ExceptionGETL('Parameter "map" is empty!')

        def tableFields = FindSection(map, "schema.complexType.${objectTypeName}.*.element") as Map<String, Map>
        if (tableFields == null || tableFields.isEmpty())
            throw new ExceptionGETL("Invalid or not found the complex type \"$objectTypeName\"!")

        def fieldList = [] as List<Field>

        tableFields.each { String fieldName, Map<String, Object> fieldParams ->
            def field = new Field(name: fieldName)

            def typeName = fieldParams.type as String
            if (typeName == null)
                throw new ExceptionGETL("Required type for field \"$fieldName\"")

            XsdType2FieldType(map, typeName, field)
			if (field.type == Field.Type.OBJECT) {
                if (field.extended.all?.element?.size() > 0 /*|| field.extended.sequence?.element?.size() > 1*/) {
                    def childFields = XsdMap2Fields(map, typeName)
                    childFields.each { Field childField ->
                        childField.name = "${field.name}.${childField.name}"
                        childField.extended.useType = typeName
                        fieldList << childField
                    }
				}
                else if (field.extended.sequence?.element?.size() > 0) {
                    field.extended.sequenceType = typeName
                    if ((field.extended.sequence as Map)?.element?.size() == 1)
						field.extended.itemType = FindSection(field.extended, 'sequence.element.*').type
                    fieldList << field
                }
                else {
                    throw new ExceptionGETL("Unknown XSD description for \"$typeName\" type!")
                }
			}
            else {
                if (fieldParams.minOccurs == 0 && fieldParams.maxOccurs == 1) field.isNull = true
                fieldList << field
            }
        }

        return fieldList
	}

    @CompileDynamic
	static void XsdType2FieldType(Map<String, Object> map, String typeName, Field field) {
        def res = XsdTypeProcess(map, typeName)

        if (res.typeName == null) throw new ExceptionGETL("Required base name for \"$typeName\" type")

        field.typeName = res.typeName
        switch (res.typeName) {
            case 'string': case 'token': case 'XMLLiteral': case 'NMTOKEN':
                field.type = Field.Type.STRING
                field.length = res.length as Integer?:1024
                break
            case 'anyURI':
                field.type = Field.Type.STRING
                field.length = res.length as Integer?:512
                break
            case 'decimal':
                field.type = Field.Type.NUMERIC
                field.precision = res.precision as Integer?:4
                field.length = res.length as Integer?:((field.precision  as Integer) + 18)
                break
            case 'float': case 'double':
                field.type = Field.Type.DOUBLE
                break
            case 'int': case 'byte': case 'integer': case 'negativeInteger': case 'nonNegativeInteger':
            case 'nonPositiveInteger': case 'positiveInteger': case 'short': case 'unsignedInt':
            case 'unsignedShort': case 'unsignedByte':
                field.type = Field.Type.INTEGER
                break
            case 'long': case 'unsignedLong':
                field.type = Field.Type.BIGINT
                break
            case 'date':
                field.type = Field.Type.DATE
                break
            case 'time':
                field.type = Field.Type.TIME
                break
            case 'dateTime':
                field.type = Field.Type.DATETIME
                break
            case 'boolean':
                field.type = Field.Type.BOOLEAN
                break
            case 'complex':
                field.type = Field.Type.OBJECT
                field.extended.putAll(res.extended as Map)
                break
            default:
                throw new ExceptionGETL("Unknown primitive XSD type \"${res.typeName}\"")
        }
    }

	@SuppressWarnings("GrUnresolvedAccess")
	@CompileDynamic
    private static Map<String, Object> XsdTypeProcess(Map<String, Object> map, String typeName) {
        if (typeName == null)
            throw new ExceptionGETL('Required \"typeName\" parameter!')

        def res = [:] as Map<String, Object>
        if (typeName.matches('xs.*[:].+')) {
            res.typeName = typeName.substring(typeName.indexOf(':') + 1)
        }
        else {
            def typeContent = (map.schema.simpleType?."$typeName") as Map
            if (typeContent != null) {
                if (typeContent.containsKey("union")) {
                    def typeParams = (typeContent.union as List)[0] as Map
                    def includeTypes = typeParams.memberTypes as String
                    if (includeTypes == null)
                        throw new ExceptionGETL("Invalid union operator for \"$typeName\" type!")

                    def listTypes = includeTypes.split(' ')
                    listTypes.each { String type ->
                        res.putAll(XsdTypeProcess(map, type))
                    }
                }
                else {
                    def typeParams = typeContent.restriction?.get(0) as Map
                    if (typeParams == null) throw new ExceptionGETL("Invalid simple type \"$typeName\"")
                    def baseType = typeParams.base as String
                    if (baseType != null)
                        res.putAll(XsdTypeProcess(map, baseType))

                    if (typeContent.annotation?.get(0)?.documentation != null)
                        res.description = typeContent.annotation.get(0).documentation as String

                    if (typeParams.maxLength?.get(0)?.value != null)
                        res.length = typeParams.maxLength.get(0).value as Integer

                    else if (typeParams.length?.get(0)?.value != null)
                        res.length = typeParams.length.get(0)?.value as Integer

                    else if (typeParams.enumeration != null) {
                        def maxLength = 0
                        typeParams.enumeration.each { Map<String, Object> enumParams ->
                            if ((enumParams.value as String)?.length() > maxLength)
                                maxLength = (enumParams.value as String)?.length()
                        }

                        if (maxLength > 0)
                            res.length = maxLength
                    }

                    if (typeParams.totalDigits?.get(0)?.value != null)
                        res.length = typeParams.totalDigits.get(0).value as Integer

                    if (typeParams.fractionDigits?.get(0)?.value != null)
                        res.precision = typeParams.fractionDigits.get(0).value as Integer
                }
            } else {
                typeContent = map.schema.complexType?."$typeName" as Map

                if (typeContent == null)
                    throw new ExceptionGETL("Type \"$typeName\" not found!")

                res.typeName = 'complex'
                res.extended = typeContent
            }
        }

        return res
    }

	/**
	 * Convert text values from the map to the values of the appropriate types
	 * @param source
	 * @return
	 */
	static Map ConvertString2Object(Map source) {
		def res = [:]
		source.each { k, v ->
			if (v instanceof Map) {
				res.put(k, ConvertString2Object(v as Map))
				return
			}

			if (!(v instanceof String)) {
				res.put(k, v)
				return
			}

			def s = v as String

			def dt = DateUtils.ParseDate('yyyy-MM-dd HH:mm:ss', s, true)
			if (dt != null) {
				res.put(k, dt)
				return
			}

			def d = DateUtils.ParseDate('yyyy-MM-dd', s, true)
			if (d != null) {
				res.put(k, d)
				return
			}

			def n = NumericUtils.String2Numeric(s, null)
			if (n != null) {
				res.put(k, n)
				return
			}

			def i = NumericUtils.String2Integer(s, null)
			if (i != null) {
				res.put(k, i)
				return
			}

			res.put(k, s)
		}

		return res
	}

	static Map<String, Object> ConfigObject2Map(ConfigObject data) {
		def res = [:] as Map<String, Object>
		data.each { k, v ->
			if (v instanceof ConfigObject)
				v = ConfigObject2Map(v)
			else if (v instanceof List) {
				def list = v as List
				for (int i = 0; i < list.size(); i++) {
					def l = list.get(i)
					if (l instanceof ConfigObject) {
						list.set(i, ConfigObject2Map(l))
					}
				}
			}
			res.put(k as String, v)
		}

		return res
	}

	/**
	 * Convert closure to map structure
	 * @param cl variable description code
	 * @return
	 */
	@SuppressWarnings("UnnecessaryQualifiedReference")
	static Map<String, Object> Closure2Map(Closure cl) {
		def cfg = new groovy.util.ConfigSlurper()
		def vars = cfg.parse(new ClosureScript(closure: cl))

		return ConfigObject2Map(vars)
	}

	/**
	 * Convert closure to map structure
	 * @param environment use the specified environment
	 * @param cl variable description code
	 * @return
	 */
	@SuppressWarnings("UnnecessaryQualifiedReference")
	static Map<String, Object> Closure2Map(String environment, Closure cl) {
		def cfg = (environment == null)?new groovy.util.ConfigSlurper():new groovy.util.ConfigSlurper(environment)
		def vars = cfg.parse(new ClosureScript(closure: cl))
		return ConfigObject2Map(vars)
	}
}