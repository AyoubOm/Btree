public class TestBTree {
	public static void main(String[] args) {
		Btree btree = new Btree(3);
		for (int i=1; i<30; i++) {
			btree.insert(i);
		}
		// btree.delete(2);
		btree.traverseHorizon();
		btree.delete(15);
		btree.delete(25);
		btree.delete(2);
		btree.delete(20);

		btree.traverseHorizon();
		btree.traverse();
	}
}