package org.mtransit.parser;

public class Pair<F, S> {

	public final F first;
	public final S second;

	public Pair(F first, S second) {
		this.first = first;
		this.second = second;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Pair)) {
			return false;
		}
		Pair<?, ?> p = (Pair<?, ?>) o;
		return p.first != null && p.first.equals(first) && p.second != null && p.second.equals(second);
	}

	@Override
	public int hashCode() {
		return (first == null ? 0 : first.hashCode()) ^ (second == null ? 0 : second.hashCode());
	}

	public static <A, B> Pair<A, B> create(A a, B b) {
		return new Pair<A, B>(a, b);
	}
}
