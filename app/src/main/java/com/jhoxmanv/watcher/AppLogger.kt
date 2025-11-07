package com.jhoxmanv.watcher

import android.util.Log

/**
 * Objeto simple y centralizado para el logging en toda la aplicación.
 * Permite activar o desactivar todos los logs de depuración con una sola variable.
 * Los logs de error siempre se mostrarán.
 */
object AppLogger {

    /**
     * Controla si los logs de depuración (`d`) están activos.
     * Por defecto, se activa solo en builds de depuración (DEBUG).
     * Se puede modificar dinámicamente si es necesario.
     */
    @JvmStatic
    var enabled: Boolean = BuildConfig.DEBUG

    /**
     * Registra un mensaje de depuración (Debug).
     * Solo se imprimirá si `AppLogger.enabled` es `true`.
     * @param tag El tag para identificar el origen del log.
     * @param message El mensaje a registrar.
     */
    fun d(tag: String, message: String) {
        if (enabled) {
            Log.d(tag, message)
        }
    }

    /**
     * Registra un mensaje de error (Error).
     * Siempre se registrará, sin importar el valor de `AppLogger.enabled`.
     * @param tag El tag para identificar el origen del log.
     * @param message El mensaje a registrar.
     * @param throwable Una excepción opcional para registrar su stack trace.
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }

    /**
     * Registra un mensaje de información (Info).
     * Solo se imprimirá si `AppLogger.enabled` es `true`.
     * @param tag El tag para identificar el origen del log.
     * @param message El mensaje a registrar.
     */
    fun i(tag: String, message: String) {
        if (enabled) {
            Log.i(tag, message)
        }
    }
}
    