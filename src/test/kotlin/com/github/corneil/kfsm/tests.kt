package com.github.corneil.kfsm

import com.github.corneil.kfsm.LockEvents.LOCK
import com.github.corneil.kfsm.LockEvents.UNLOCK
import com.github.corneil.kfsm.LockStates.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class KfsmTests {

    @Test
    fun `test creation of fsm`() {
        // given
        val definition = StateMachine<LockStates, LockEvents, Lock>()
        definition.deriveInitialState = { if (it.locked) LOCKED else UNLOCKED }
        definition.transition(UNLOCK, LOCKED, UNLOCKED) { context ->
            context.unlock()
        }
        definition.transition(UNLOCK, DOUBLE_LOCKED, LOCKED) { context ->
            context.doubleUnlock()
        }
        definition.transition(LOCK, UNLOCKED, LOCKED) { context ->
            context.lock()
        }
        definition.transition(LOCK, LOCKED, DOUBLE_LOCKED) { context ->
            context.doubleLock()
        }

        val lock = Lock(true)
        // when
        val fsm = definition.instance(LOCKED, lock)
        // then
        assertTrue { fsm.currentState == LOCKED }
        assertTrue { lock.locked }
        // when
        fsm.event(UNLOCK)
        // then
        assertTrue { fsm.currentState == UNLOCKED }
        assertTrue { !lock.locked }
        try {
            // when
            fsm.event(UNLOCK)
            fail("Expected an exception")
        } catch (x: Throwable) {
            println("Expected:$x")
            // then
            assertEquals("Statemachine doesn't provide for UNLOCK in UNLOCKED", x.message)
        }
        // when
        fsm.event(LOCK)
        // then
        assertTrue { fsm.currentState == LOCKED }
        assertTrue { lock.locked }
        // when
        fsm.event(LOCK)
        // then
        assertTrue { fsm.currentState == DOUBLE_LOCKED }
        assertTrue { lock.locked }
        try {
            // when
            fsm.event(LOCK)
            fail("Expected an exception")
        } catch (x: Throwable) {
            println("Expected:$x")
            // then
            assertEquals("Statemachine doesn't provide for LOCK in DOUBLE_LOCKED", x.message)
        }
    }

    @Test
    fun `test dsl creation of fsm`() {
        // given
        val definition = StateMachine<LockStates, LockEvents, Lock>().dsl {
            initial { if (it.locked) LOCKED else UNLOCKED }
            event(UNLOCK) {
                state(LOCKED to UNLOCKED) { context ->
                    context.unlock()
                }
                state(DOUBLE_LOCKED to LOCKED) { context ->
                    context.doubleUnlock()
                }
                state(UNLOCKED) {
                    error("Already Unlocked")
                }
            }
            event(LOCK) {
                state(UNLOCKED to LOCKED) { context ->
                    context.lock()
                }
                state(LOCKED to DOUBLE_LOCKED) { context ->
                    context.doubleLock()
                }
                state(DOUBLE_LOCKED) {
                    error("Already Double Locked")
                }
            }
        }.build()

        val lock = Lock(true)
        // when
        val fsm = definition.instance(LOCKED, lock)
        // then
        assertTrue { fsm.currentState == LOCKED }
        assertTrue { lock.locked }
        // when
        fsm.event(UNLOCK)
        // then
        assertTrue { fsm.currentState == UNLOCKED }
        assertTrue { !lock.locked }

        try {
            // when
            fsm.event(UNLOCK)
            fail("Expected an exception")
        } catch (x: Throwable) {
            println("Expected:$x")
            // then
            assertEquals("Already Unlocked", x.message)
        }
        // when
        fsm.event(LOCK)
        // then
        assertTrue { fsm.currentState == LOCKED }
        assertTrue { lock.locked }
        // when
        fsm.event(LOCK)
        // then
        assertTrue { fsm.currentState == DOUBLE_LOCKED }
        assertTrue { lock.locked }
        try {
            // when
            fsm.event(LOCK)
            fail("Expected an exception")
        } catch (x: Throwable) {
            println("Expected:$x")
            // then
            assertEquals("Already Double Locked", x.message)
        }
    }
}
