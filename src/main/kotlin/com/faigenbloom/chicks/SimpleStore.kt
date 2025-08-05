package com.faigenbloom.chicks

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

object SimpleStore {
    private val file = File("data.properties")
    private val props = Properties()

    init {
        // Создаем файл, если его нет
        if (!file.exists()) {
            file.createNewFile()
        }
        load()
    }

    private fun load() {
        FileInputStream(file).use { fis ->
            props.load(fis)
        }
    }

    private fun save() {
        FileOutputStream(file).use { fos ->
            props.store(fos, null)
        }
    }

    fun put(key: String, value: String) {
        props[key] = value
        save()
    }

    fun get(key: String): String? {
        return props.getProperty(key)
    }

    fun getAll(): Map<String, String> {
        return props.entries.associate { it.key.toString() to it.value.toString() }
    }

    fun delete(key: String) {
        props.remove(key)
        save()
    }
}
