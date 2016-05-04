package ch.inftec.ju.util.function;

public class Predicates {
	public static final Predicate ALWAYS_TRUE = new Predicate() {
		@Override
		public boolean test(Object o) {
			return true;
		}
	};
}
