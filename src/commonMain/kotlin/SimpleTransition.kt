/*
 * Copyright (c) 2019. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.jumpco.open.kfsm

/**
 * @suppress
 * Represents a transition from a given state and event.
 * @param startState The given state
 * @param event The given event
 * @param targetState when optional represents an internal transition
 * @param action An optional lambda that will be invoked.
 */
open class SimpleTransition<S, E, C, A, R>(
    internal val startState: S,
    internal val event: E?,
    targetState: S?,
    targetMap: String?,
    automatic: Boolean,
    type: TransitionType,
    action: StateAction<C, A, R>?
) : Transition<S, E, C, A, R>(targetState, targetMap, automatic, type, action)
