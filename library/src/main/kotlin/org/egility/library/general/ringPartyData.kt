/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.general

import org.egility.library.dbobject.*

/**
 * Created by mbrickman on 28/10/15.
 */

enum class ScrimeMode { NORMAL, EDIT, RERUN_TIME, RERUN_SCRATCH }


object ClassData{
    var agilityClass = AgilityClass()
    var targetEntry = Entry()
    var activeChildClass = AgilityClass()
}

object ringPartyData {

    private val _entry = Entry()
    private val _team = Team()
    private var _ring = Ring()
    private var _agilityClass = AgilityClass()

    private var idTeam = 0
    private var idEntry = 0

    var scrimeMode: Boolean = false

    var mode = ScrimeMode.NORMAL

    val editMode: Boolean
        get() = mode == ScrimeMode.EDIT


    val reRunTimeMode: Boolean
        get() = mode == ScrimeMode.RERUN_TIME

    val reRunScratchMode: Boolean
        get() = mode == ScrimeMode.RERUN_SCRATCH

    val reRunMode: Boolean
        get() = mode == ScrimeMode.RERUN_SCRATCH || mode == ScrimeMode.RERUN_TIME

    init {
        _entry.team.joinToParent()
        _entry.agilityClass.joinToParent()
        _entry.team.competitor.joinToParent()
        _entry.team.dog.joinToParent()

        _team.competitor.joinToParent()
        _team.dog.joinToParent()

        _entry.reference = "EntryServicesData._entry"
        _team.reference = "EntryServicesData._team"

        _ring.agilityClass.joinToParent()
    }

    fun reset(all: Boolean = true) {
        mode = ScrimeMode.NORMAL
        idTeam = 0
        idEntry = 0
        _team.release()
        _entry.release()
        if (all) {
            _ring.release()
            _agilityClass.release()
            jumpHeight.release()
        }
    }

    val isEntrySelected: Boolean
        get() = idEntry != 0

    fun selectTeam(idTeam: Int) {
        debug("Runner", "teamSelected (idTeam=$idTeam)")
        reset(all = false)
        this.idTeam = idTeam
    }

    fun entrySelected(idEntry: Int) {
        debug("Runner", "entrySelected (idEntry=$idEntry)")
        reset(all = false)
        this.idEntry = idEntry
    }

    fun selectClassEntry(idAgilityClass: Int) {
        debug("Runner", "selectClassEntry (idAgilityClass=$idAgilityClass)")
        if (entry.isOffRow || entry.idAgilityClass != idAgilityClass || entry.idTeam != idTeam) {
            entry.select("entry.idAgilityClass=$idAgilityClass AND entry.idTeam = $idTeam")
            if (!_entry.found()) {
                entry.select("entry.idAgilityClass=$idAgilityClass AND entry.idTeam IN (${team.idTeamList})")
            }
            if (_entry.found()) {
                // must use _entry here to prevent re-seek
                idEntry = _entry.id
                idTeam = entry.idTeam
                debug("Runner", "selectClassEntry - FOUND! (idEntry=$idEntry)")
            } else {
                idEntry = 0
            }
        }
    }

    val entry: Entry
        get() {
            if ((_entry.isOffRow || _entry.id != idEntry) && idEntry > 0) {
                _entry.find(idEntry)
                if (_entry.found()) {
                    idTeam = _entry.idTeam
                }
            }
            return _entry
        }

    val team: Team
        get() {
            if (idEntry > 0) {
                return entry.team
            } else {
                if (_team.isOffRow || _team.id != idTeam) {
                    _team.find(idTeam)
                }
                return _team
            }
        }

    val ring: Ring
        get () {
            return _ring
        }

    val agilityClass: AgilityClass
        get () {
            return _agilityClass
        }


    fun setRunner(idEntry: Int, runningOrder: Int, runner: String, runnerHeightCode: String) {
        _ring.idEntry = idEntry
        _ring.runningOrder = runningOrder
        _ring.runner = runner
        _ring.runnerHeightCode = runnerHeightCode
        _ring.post()
    }

    val jumpHeight = Height()

    var jumpHeightCode: String
        get() {
            if (jumpHeight.rowCount == 0) {
                jumpHeight.select("true", "heightCode")
            }
            return if (jumpHeight.isOnRow) jumpHeight.code else ""
        }
        set(value) {
            if (jumpHeight.rowCount == 0) {
                jumpHeight.select("true", "heightCode")
            }
            if (value != jumpHeight.code) {
                jumpHeight.beforeFirst()
                while (jumpHeight.next()) {
                    if (jumpHeight.code == value) {
                        return
                    }
                }
            }
        }
    
    val group: String
        get() = ring.group

    var scrimeJumpHeightCode: String
        get() {
            return ring.heightCode
        }
        set(value) {
            ring.heightCode = value
            ring.post()
            jumpHeightCode = value

        }

    fun setRingGroup(group: String) {
        ring.group = group
        ring.heightCode = ring.agilityClass.firstJumpHeightCode
        ring.post()
        jumpHeightCode = ring.heightCode
    }

    fun setRingScrimeHeight(ringHeightCode: String) {
        ring.heightCode = ringHeightCode
        ring.post()
        jumpHeightCode = ringHeightCode
    }


    fun addLateEntry(enterHeightCode: String, isEnterClearRoundOnly: Boolean): Int {
        return ringPartyData.agilityClass.enter(
            idTeam = idTeam,
            heightCode = enterHeightCode,
            clearRoundOnly = isEnterClearRoundOnly,
            entryType = ENTRY_LATE_CREDITS,
            progress = PROGRESS_BOOKED_IN,
            idAccount = team.dog.idAccount,
            timeEntered = now
        )
    }


//============================================================================================

    fun setRingNumber(ringNumber: Int, force: Boolean = false) {
        if (ring.isOffRow || ringNumber != ring.number || force) {
            ring.select(Competition.current.id, today, ringNumber)
        }
    }

    fun setAgilityClass(idAgilityClass: Int) {
        if (agilityClass.isOffRow || idAgilityClass != agilityClass.id) {
            agilityClass.find(idAgilityClass)
            setRingNumber(agilityClass.ringNumber)
            jumpHeightCode = agilityClass.firstJumpHeightCode
        } else {
            agilityClass.post()
            agilityClass.refresh()
        }
    }

    fun syncRing() {
        val idAgilityClass = agilityClass.id
        ring.refresh()
        if (ring.agilityClass.isChild) {
            ring.agilityClass.parentClass.refresh()
        }

        if (ring.idAgilityClass > 0 && ring.idAgilityClass != idAgilityClass) {
            setAgilityClass(ring.idAgilityClass)
            if (!ring.agilityClass.isLocked && ring.agilityClass.progress == CLASS_PENDING) {
                ring.agilityClass.progress = CLASS_PREPARING
                ring.post()
            }
        }
    }

    fun syncRingBug() {
        val idAgilityClass = ring.idAgilityClass
        ring.refresh()
        if (ring.idAgilityClass > 0) {
            setAgilityClass(ring.idAgilityClass)
            if (ring.agilityClass.isLocked) {
                ring.agilityClass.progress = CLASS_PENDING
                ring.post()
            } else if (ring.agilityClass.progress == CLASS_PENDING) {
                ring.agilityClass.progress = CLASS_PREPARING
                ring.post()
            }
        }
        if (ring.idAgilityClass != idAgilityClass) {
            jumpHeightCode = ring.heightCode
        }
    }
}

