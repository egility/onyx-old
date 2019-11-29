/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.general

/**
 * Created by mbrickman on 12/08/18.
 */

inline fun <reified T> matrix2d(height: Int, width: Int, initialize: () -> T) =
        Array(height) { Array(width) { initialize() } }

private data class StepCost(val worstCost: Int = -1, val combinedCost: Int = -1, val weakLink: String="", val ok: Boolean = true)

class AcuCluster(val tags: ArrayList<String>) {
    val cost = matrix2d(tags.size, tags.size) { -1 }
    var bestRoute = ArrayList<String>()
    var bestWorstCost = Int.MAX_VALUE
    var bestCost = Int.MAX_VALUE
    var weakestLink = ""

    fun addCost(tagA: String, tagB: String, value: Int) {
        val a = tags.indexOf(tagA)
        val b = tags.indexOf(tagB)
        if (a >= 0 && b >= 0) {
            cost[a][b] = value
        }
    }

    private fun getCost(tagA: String, tagB: String): StepCost {
        val a = tags.indexOf(tagA)
        val b = tags.indexOf(tagB)

        if (a == b || cost[a][b] == -1 || cost[b][a] == -1) {
            return StepCost(ok = false)
        } else {
            val aToB = cost[a][b]
            val bToA = cost[b][a]
            val weekLink = if (aToB>=bToA) "$tagA->$tagB" else "$tagB->$tagA"
            return StepCost(maxOf(aToB, bToA), aToB + bToA, weekLink)
        }
    }

    fun printRoute(route: ArrayList<String>, worst: Int, cost: Int, weakLink: String) {
        var line: String = ""
        for (tag in route) {
            line = line.append(tag)
        }
        debug("cluster", "$line ($cost), $weakLink ($worst)")
    }

    fun printRoute2(route: ArrayList<String>, remain: ArrayList<String>) {
        var line: String = ""
        var line2: String = ""
        for (tag in route) {
            line = line.append(tag)
        }
        for (tag in remain) {
            line2 = line2.append(tag)
        }
        debug("cluster", "Route: $line, Remain: $line2")
    }

    fun doStep(route: ArrayList<String>, remain: ArrayList<String>, worst: Int = 0, cost: Int = 0, weakLink: String="") {
        val here = route.last()
        //printRoute2(route, remain)
        if (remain.size > 0) {
            for (there in remain) {
                val stepCost = getCost(here, there)
                val newWorst = maxOf(stepCost.worstCost, worst)
                if (stepCost.ok && newWorst < bestWorstCost) {
                    val newRoute = ArrayList<String>(route)
                    val newRemain = ArrayList<String>(remain)
                    val newCost = cost + stepCost.combinedCost
                    val newWeakLink = if (newWorst==stepCost.worstCost)  stepCost.weakLink else weakLink
                    newRoute.add(there)
                    newRemain.removeAt(newRemain.indexOf(there))
                    doStep(newRoute, newRemain, newWorst, newCost, newWeakLink)
                }
            }
        } else {
            val there = route.first()
            val stepCost = getCost(here, there)
            if (stepCost.ok) {
                val routeWorst = maxOf(stepCost.worstCost, worst)
                val routeWeakLink = if (routeWorst==stepCost.worstCost)  stepCost.weakLink else weakLink
                val routeCost = cost + stepCost.combinedCost
                if (routeWorst < bestWorstCost || (routeWorst == bestWorstCost && routeCost < bestCost)) {
                    bestWorstCost = routeWorst
                    bestCost = routeCost
                    bestRoute = route
                    weakestLink = routeWeakLink
                    printRoute(route, routeWorst, routeCost, weakestLink)
                }
            } else {
                // can't join up chain
            }
        }
    }

    fun goFigure() {
        synchronized(this) {
            if (tags.size <= 3) {
                debug("cluster", "Fixed route (${tags.size})") 
                bestWorstCost = 1
                bestCost = 1
                bestRoute = ArrayList(tags)
            } else {
                debugTime("cluster", "goFigure (${tags.size})") {
                    val route = ArrayList<String>()
                    val remain = ArrayList<String>()
                    route.add(tags[0])
                    for (i in 1..tags.size - 1) {
                        remain.add(tags[i])
                    }
                    doStep(route, remain)
                }
            }
        }
    }

    fun populateJson(node: JsonNode) {
        node.clear()
        synchronized(this) {
            var line: String = ""
            for (tag in bestRoute) {
                line = line.append(tag)
            }

            node["route"]=bestRoute.asCommaList()
            node["cost"]=bestCost
            node["weakestLink"]=weakestLink
            node["linkCost"]=bestWorstCost
        }

    }
}

