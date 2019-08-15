/*
 * Copyright (c) 2019. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.jumpco.open.kfsm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DetailTests {
    enum class TestStates {
        STATE1,
        STATE2,
        STATE3
    }

    enum class TestEvents {
        EVENT1,
        EVENT2,
        EVENT3
    }

    class TestContext(var state: Int) {
        fun defaultAction() {
            println("defaultAction")
        }

        fun defaultEntry() {
            println("defaultEntry")
        }

        fun defaultExit() {
            println("defaultEntry")
        }

        fun action1() {
            println("action1")
        }

        fun action2() {
            println("action2")
        }

        fun entry1() {
            println("entry1")
        }

        fun entry2() {
            println("entry2")
        }

        fun exit2() {
            println("exit2")
        }

        fun exit3() {
            println("exit3")
        }

        override fun toString(): String {
            return "TestContext(state=$state)"
        }
    }

    class TestDetailFSM(context: TestContext) {
        val fsm = definition.create(context)
        fun allowedEvents() = fsm.allowed()
        fun eventAllowed(event: TestEvents, includeDefaults: Boolean = false) =
            fsm.eventAllowed(event, includeDefaults)

        fun event1(msg: String) {
            println("--event1")
            fsm.sendEvent(TestEvents.EVENT1, msg)
        }

        fun event2(msg: String) {
            println("--event2")
            fsm.sendEvent(TestEvents.EVENT2, msg)
        }

        fun event3(msg: String) {
            println("--event3")
            fsm.sendEvent(TestEvents.EVENT3, msg)
        }
        companion object {
            val definition = StateMachineBuilder<TestStates, TestEvents, TestContext>(TestStates.values().toSet())
                .stateMachine {
                    initialMap {
                        mutableListOf<StateMapItem<TestStates>>().apply {
                            when (state) {
                                1 -> add(TestStates.STATE1 to "default")
                                2 -> add(TestStates.STATE2 to "default")
                                3 -> add(TestStates.STATE3 to "default")
                                else -> error("Invalid state $state")
                            }
                        }
                    }
                    default {
                        entry { startState, targetState, args ->
                            val msg = args[0] as String
                            println("entering:to $targetState from $startState for:$this:$msg")
                            defaultEntry()
                        }
                        exit { startState, targetState, args ->
                            val msg = args[0] as String
                            println("exiting:from $targetState to $startState for:$this:$msg")
                            defaultExit()
                        }
                        transition(TestEvents.EVENT1 to TestStates.STATE1) { args ->
                            val msg = args[0] as String
                            println("default:EVENT1 to STATE1 for $this:$msg")
                            action1()
                            state = 1
                        }
                        transition(TestEvents.EVENT2 to TestStates.STATE2) { args ->
                            val msg = args[0] as String
                            println("default:on EVENT2 to STATE2 for $this:$msg")
                            action2()
                            state = 2
                        }
                        transition(TestEvents.EVENT3 to TestStates.STATE3) { args ->
                            val msg = args[0] as String
                            println("default:on EVENT3 to STATE3 for $this:$msg")
                            defaultAction()
                            state = 3
                        }
                        action { currentState, event, args ->
                            val msg = args[0] as String
                            println("default:$event from $currentState for $this:$msg")
                            defaultAction()
                        }
                    }
                    stateMap("map1", setOf(TestStates.STATE1, TestStates.STATE2)) {
                        state(TestStates.STATE1) {
                            popTransition(TestEvents.EVENT1) {
                                println("pop")
                            }
                        }
                        state(TestStates.STATE2) {
                            automatic(TestStates.STATE1) {
                                println("automatic -> TestStates.STATE1")
                            }
                        }
                    }
                    state(TestStates.STATE1) {
                        transition(TestEvents.EVENT1) {
                            action1()
                        }
                        entry { _, _, _ ->
                            entry1()
                        }
                    }
                    state(TestStates.STATE2) {
                        entry { _, _, _ ->
                            entry2()
                        }
                        transition(TestEvents.EVENT2, guard = { state == 2 }) { args ->
                            val msg = args[0] as String
                            println("EVENT2:guarded:from STATE2 for $this:$msg")
                            action2()
                        }
                        pushTransition(TestEvents.EVENT1, "map1", TestStates.STATE2) { args ->
                            val msg = args[0] as String
                            println("EVENT1:push:from STATE2 for $this:$msg")
                            action2()
                        }
                        exit { _, _, _ ->
                            exit2()
                        }
                    }
                    state(TestStates.STATE3) {
                        exit { _, _, _ ->
                            exit3()
                        }
                        transition(TestEvents.EVENT2, guard = { state == 2 }) {
                            error("should never be called")
                        }
                    }
                }.build()
        }
    }

    @Test
    fun `test actions`() {
        // given
        val context = TestContext(3)
        val fsm = TestDetailFSM(context)
        var msgNo = 1
        // when
        fsm.event1((++msgNo).toString())
        // then
        assertEquals(1, context.state)
        assertTrue(fsm.eventAllowed(TestEvents.EVENT1))
        // when
        fsm.event2((++msgNo).toString())
        // then
        assertEquals(2, context.state)
        assertTrue(fsm.eventAllowed(TestEvents.EVENT2))
        // when
        fsm.event2((++msgNo).toString())
        assertEquals(2, context.state)
        // then
        fsm.event3((++msgNo).toString())
        assertEquals(3, context.state)
        assertEquals(fsm.allowedEvents(), setOf(TestEvents.EVENT2))
        // when
        fsm.event2((++msgNo).toString())
        assertEquals(3, context.state)
        // then
        fsm.event1((++msgNo).toString())
        assertEquals(1, context.state)
        assertEquals(fsm.allowedEvents(), setOf(TestEvents.EVENT1))
        fsm.event2((++msgNo).toString())
        assertEquals(2, context.state)
        assertEquals(fsm.fsm.currentState, TestStates.STATE2)
        fsm.event1((++msgNo).toString())
        assertEquals(fsm.fsm.currentStateMap.name, "map1")
        assertEquals(fsm.fsm.currentState, TestStates.STATE1)
        fsm.event1((++msgNo).toString())
        assertNull(fsm.fsm.currentStateMap.name)
        assertEquals(fsm.fsm.currentState, TestStates.STATE2)
    }
}