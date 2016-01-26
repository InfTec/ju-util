package ch.inftec.ju.util.function;

public class JuPredicates {
	public static final Predicate ALWAYS_TRUE = new Predicate() {
		@Override
		public boolean test(Object o) {
			return true;
		}
	};
}
