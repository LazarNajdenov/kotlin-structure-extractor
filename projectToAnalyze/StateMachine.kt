package ch.usi.si.msde.edsl.assignment_05_template.model


interface MyInterface {
    fun bar()
    fun foo()
}
/**
 * A name for events, commands, and states.
 *
 * Name representations are case insensitive.
 * @param value the string representing a name.
 */
data class Name(val value: String)

/**
 * A language entity that has a name.
 */
abstract class NamedEntity(var name: Name? = null) {

    class Nested {
        val variable: Int = 0
        fun foo() {

        }
    }

    inner class NestedInner: MyInterface {
        val variableInner: Int = 0
        fun fooInner(aValue: Int) {

        }
    }

    companion object {
        val aCompanionVariable: String? = null
    }
    fun name(nameString: String) {
        this.name = Name(nameString)
    }
}

abstract class AbstractEvent(name: Name? = null): NamedEntity(name)

/**
 * This is a named command that can be sent to the environment by states.
 *
 * @param name The name of the command.
 */
class Command(name: Name? = null): AbstractEvent(name), MyInterface {
    override fun toString() = "c(${name?.value})"
}

/**
 * Named environment event that triggers a transition.
 *
 * @param name The name of the event.
 * @param guard The optional guard on counters that activates the event.
 * @param effect the optional effect on counters of this transition.
 */
class Event(name: Name? = null, var guard: () -> Boolean = { true }, var effect: () -> Unit = {}): AbstractEvent(name) {

    override fun toString() = "e(${name?.value})"
    
    fun guard(lambda: () -> Boolean) {
        this.guard = lambda
    }
    
    fun effect(lambda: () -> Unit) {
        this.effect = lambda
    }

    infix fun to(state: State): EventToState {
        return EventToState(this, state)
    }

    operator fun rangeTo(state: State): EventToState {
        return EventToState(this, state)
    }
}

/**
 * A state on the state machine.
 *
 * @param name The name of the state.
 * @param commands The set of commands sent by this state to the environment.
 */
 class State(name: Name? = null, var initial: Boolean = false, val commands: Set<Command> = setOf()): NamedEntity(name) {

    override fun toString() = "s(${name?.value},cs(${commands}))"

    fun initial() {
        this.initial = true
    }
}

/**
 * A transition between to states, triggered by an event.
 *
 * @param source the source state.
 * @param trigger the events that triggers the transition from the source state.
 * @param target the target state.
 */
data class Transition(val source: State, val trigger: Event, val target: State) {
    override fun toString() = "${source} -${trigger}-> ${target}"
}

/**
 * A state machine.
 *
 * @param initialState the initial state of the machine.
 * @param transitions a list of transitions.
 * @param counters a set of counters.
 */
data class StateMachine(var initialState: State? = null,
                        var transitions: List<Transition> = listOf()) {

    fun state(init: State.() -> Unit): State {
        val state = State()
        state.init()
        if (state.initial) {
            this.initialState = state
        }
        return state
    }

    fun counter(init: Counter.() -> Unit): Counter {
        val counter = Counter()
        counter.init()
        return counter
    }

    fun event(init: Event.() -> Unit): Event {
        val event = Event()
        event.init()
        return event
    }

    fun transitions(init: Transitions.() -> Unit): Transitions {
        val transitions = Transitions()
        transitions.init()
        this.transitions = transitions.transitions
        return transitions
    }
}

/**
 * An event and State pair.
 */
class EventToState(var event: Event? = null, var target: State? = null) {

}

/**
 * Represents a set of transitions.
 */
class Transitions(var transitions: MutableList<Transition> = mutableListOf<Transition>()) {
    operator fun State.invoke(lambda: State.() -> EventToState) {
        val eventToState = lambda()
        val transition = eventToState.event?.let { eventToState.target?.let { state -> Transition(this, it, state) } }
        if (transition != null) {
            this@Transitions.transitions.add(transition)
        }
    }
}

class Counter(name: Name? = null, var initialValue: Int = 0): NamedEntity(name) {
    var value: Int = initialValue
        get() = field
        set(value) {
            field = value
        }

    constructor(anotherValue: Int) {
        value = value + anotherValue
    }

    fun initialValue(value: Int) {
        initialValue = value
    }

    operator fun compareTo(i: Int): Int {
        return value.compareTo(i)
    }

    operator fun plus(i: Int): Counter {
        value += i
        return this
    }

    operator fun minus(i: Int): Counter {
        value -= i
        return this
    }

    operator fun times(i: Int): Counter {
        value *= i
        return this
    }

    operator fun div(i: Int): Counter {
        value /= i
        return this
    }

    operator fun rem(i: Int): Counter {
        value %= i
        return this
    }
}


