package io.x.ledger.utils

import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.query.Update
import org.springframework.data.relational.core.sql.SqlIdentifier
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaField

fun <T: Any> Map<String, Any>.toObject(klass: KClass<T>): T =
    mapToObject(this, klass)

fun <T: Any, U: Any> merge(obj: T, update: U, klassObj: KClass<T>, klassUpdate: KClass<U>): T {
    val map = objectToMap(obj, klassObj)
    val updateMap = objectToMap(update, klassUpdate)
    val merged = mergeMaps(map, updateMap)
    return merged.toObject(klassObj)
}

fun <T: Any> toUpdate(obj: T, klass: KClass<T>): Update {
    val map = objectToMap(obj, klass)
    val updateMap = map.entries.map {
        SqlIdentifier.quoted(it.key) to it.value
    }.toMap()
    return Update.from(updateMap)
}

private fun mergeMaps(map: Map<String, Any>, update: Map<String, Any>): Map<String, Any> =
    map.entries.map {
        val updatedVal = update[it.key]
        if (updatedVal == null) {
            it.key to it.value
        } else {
            it.key to updatedVal
        }
    }.toMap()


private fun <T: Any> objectToMap(obj: T, klass: KClass<T>): Map<String, Any> =
    klass.memberProperties.filter {
        it(obj) != null
    }.map {
        modelNameToMapKey(it) to it(obj)!!
    }.toMap()



private fun <T: Any> mapToObject(source: Map<String, Any>, klass: KClass<T>): T {
    val cons = klass.primaryConstructor!!

    val values = klass.memberProperties.map {
        it.name to source[modelNameToMapKey(it)]
    }.toMap()
    val arguments = cons.parameters.map { values[it.name] }.toTypedArray()
    return cons.call(*arguments)
}

private fun <T: Any> modelNameToMapKey(property: KProperty1<T, *>): String {
    val columnAnnotation = property.javaField?.getAnnotation(Column::class.java)
    if (columnAnnotation != null) {
        return columnAnnotation.value
    }
    return toSnakeCase(property.name)
}

private fun toSnakeCase(s: String): String {
    val sb = StringBuilder()
    for (i in s.indices) {
        val c = s[i]
        if (c.isUpperCase()) {
            sb.append('_')
            sb.append(c.toLowerCase())
        } else {
            sb.append(c)
        }
    }
    return sb.toString()
}