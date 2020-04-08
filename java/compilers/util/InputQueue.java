package compilers.util;

import compilers.Symbol;
import org.apache.commons.collections4.queue.AbstractQueueDecorator;
import java.util.ArrayDeque;

public class InputQueue extends AbstractQueueDecorator<Symbol> {

	public InputQueue() {
		super(new ArrayDeque<>());
	}

	@Override
	public Symbol peek() {
		Symbol p = super.peek();
		if (p == null)
			return Symbol.EOF;
		return p;
	}

	@Override
	public Symbol poll() {
		Symbol p = super.poll();
		if (p == null)
			return Symbol.EOF;
		return p;
	}
}
