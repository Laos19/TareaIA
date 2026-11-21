package com.tareia.app.ia

import android.content.Context

// =====================================================================================
// PATRÓN ABSTRACT FACTORY (creacional) — LA FÁBRICA ABSTRACTA
// -------------------------------------------------------------------------------------
// Problema: los tres servicios de IA (OCR, extractor, generador de plan) deben venir del
// MISMO proveedor (mismas credenciales, mismo formato). RNF-06 / RNF-09 piden poder
// cambiar de proveedor sin rediseñar la app.
//
// Solución: una interfaz que sabe crear la FAMILIA completa. Cada proveedor tiene su
// fábrica concreta:
//   FabricaProveedorA -> Google: Gemini mediante Firebase AI Logic (la de siempre).
//   FabricaProveedorB -> cualquier API compatible con OpenAI, configurada en Más > IA.
//
// Roles:
//   AbstractFactory  -> FabricaServiciosIA (esta interfaz)
//   ConcreteFactory  -> FabricaProveedorA, FabricaProveedorB
//   AbstractProduct  -> ReconocedorOCR, ExtractorParametros, GeneradorPlanIA
//   Cliente          -> GestorAnalisisIA (y PlanificacionConIA)
//
// Flujo: la fábrica se elige UNA sola vez (en Dependencias) y se le entrega al gestor.
// El gestor le pide los servicios (crear...()) y los usa sin saber de qué proveedor son.
// =====================================================================================

/** «AbstractFactory» Crea la familia de servicios de IA de un proveedor. */
interface FabricaServiciosIA {
    /** Nombre del proveedor, para mostrarlo en pantalla. */
    val nombreProveedor: String

    fun crearReconocedorOCR(): ReconocedorOCR
    fun crearExtractor(): ExtractorParametros
    fun crearGeneradorPlan(): GeneradorPlanIA

    companion object {
        // Es el ÚNICO lugar de la app donde se nombran la fábricas concretas
        fun seleccionar(context: Context): FabricaServiciosIA {
            val config = AiSettings.load(context)
            return if (config.estaCompleta()) FabricaProveedorB(context.applicationContext, config)
            else FabricaProveedorA(context.applicationContext)
        }
    }
}
