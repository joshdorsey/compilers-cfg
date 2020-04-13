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
		if (super.isEmpty())
			return Symbol.EOF;
		return super.peek();
	}

	@Override
	public Symbol poll() {
		if (super.isEmpty())
			return Symbol.EOF;
		return super.poll();
	}
}
