/*
 * Copyright (c) 2019. Open JumpCO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.jumpco.open.kfsm

class StateMachineDefinition<S : Enum<S>, E : Enum<E>, C>(
    private val deriveInitialState: StateQuery<C, S>?,
    val transitionRules: Map<Pair<S, E>, TransitionRules<S, E, C>>,
    val defaultTransitions: Map<E, DefaultTransition<E, S, C>>,
    val entryActions: Map<S, DefaultChangeAction<C, S>>,
    val exitActions: Map<S, DefaultChangeAction<C, S>>,
    val defaultActions: Map<S, DefaultStateAction<C, S, E>>,
    val globalDefault: DefaultStateAction<C, S, E>?,
    val defaultEntryAction: DefaultChangeAction<C, S>?,
    val defaultExitAction: DefaultChangeAction<C, S>?
) {
    /**
     * This function will create a state machine instance provided with content and optional initialState.
     * @param context The context will be provided to actions
     * @param initialState If this is not provided the function defined in `initial` will be invoked to derive the initialState.
     * @see StateMachineBuilder.initial
     */
    fun create(context: C, initialState: S? = null): StateMachineInstance<S, E, C> {
        return StateMachineInstance(
            context,
            this,
            initialState ?:
                        deriveInitialState?.invoke(context) ?: error("Definition requires deriveInitialState")
        )
    }

    /**
     * This function will provide the set of allowed events given a specific state. It isn't a guarantee that a
     * subsequent transition will be successful since a guard may prevent a transition. Default state handlers are not considered.
     * @param given The specific state to consider
     * @param includeDefault When `true` will include default transitions in the list of allowed events.
     */
    fun allowed(given: S, includeDefault: Boolean = false): Set<E> {
        val result = transitionRules.entries.filter {
            it.key.first == given
        }.map {
            it.key.second
        }.toSet()
        if (includeDefault && defaultTransitions.isNotEmpty()) {
            return result + defaultTransitions.keys
        }
        return result
    }

    /**
     * This function will provide an indicator if an event is allow for a given state.
     * When no state transition is declared this function will return false unless `includeDefault` is true and
     * there is a default transition of handler for the event.
     */
    fun eventAllowed(event: E, given: S, includeDefault: Boolean): Boolean =
        (includeDefault &&
                hasDefaultStateHandler(given)) ||
                allowed(given, includeDefault).contains(event)


    /**
     * This function will provide an indicator if a default action has been defined for a given state.
     */
    private fun hasDefaultStateHandler(given: S) = defaultActions.contains(given)

}
