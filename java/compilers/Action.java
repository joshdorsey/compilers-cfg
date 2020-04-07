package compilers;

abstract class Action {
	private boolean accepting;

	protected Action(boolean accept) {
		accepting = accept;
	}
	
	boolean isAccepting() {
		return accepting;
	}

	abstract void doAction();

	static class Shift extends Action {
		private int stateNum;

		Shift(int goToState) {
			super(false);
			stateNum = goToState;
		}

		@Override
		void doAction() {}

		@Override
		public String toString() {
			return "sh-" + stateNum;
		}
	}

	static class Reduce extends Action {
		private CFG.Rule rule;

		Reduce(CFG.Rule reduceWith, boolean accept) {
			super(accept);
			rule = reduceWith;
		}

		@Override
		void doAction() {}

		@Override
		public String toString() {
			return "r-" + rule;
		}
	}
}
