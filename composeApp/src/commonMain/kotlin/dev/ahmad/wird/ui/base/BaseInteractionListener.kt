package dev.ahmad.wird.ui.base

/**
 * Marker for a feature's interaction contract. Each feature declares one interface
 * extending this, and its ViewModel implements it, so a screen takes a single
 * listener rather than a drift of loose lambdas.
 */
interface BaseInteractionListener
