/*
Author : Ayoub Omari

B-tree implementation : keys are of type integer, which is the most used type to index database tables


Current Bug : we have to merge in the node we will recurse in, not always the left. The special case is when we will
recurse to the last child
*/

import java.util.LinkedList;

public class Btree {

	private BtreeNode root;
	private int minDegree;

	public Btree(int minDeg) {
		if (minDeg < 2) {
			throw new IllegalArgumentException("The minimum degree of a B-tree is 2, "+minDeg+" given.");
		}
		this.minDegree = minDeg;
		this.root = new BtreeNode(minDegree, true); 
	}

	public NodeAndIndex search(int key) {
		return root.search(key);
	}

	public void insert(int key) {
		// The insertion is done by splitting the encountered full nodes until a leaf into which we can insert the key
		if (root.getNbKeys() == 2*minDegree-1) { //The root is full 
			BtreeNode newRoot = new BtreeNode(minDegree, false);
			newRoot.getChildren()[0] = root;
			this.root = newRoot;
			System.out.println("--Splitting root");
			newRoot.splitChild(0);
			newRoot.insertNonFull(key);
		} else {
			root.insertNonFull(key);
		}
	}

	public void delete(int key) {
		int rootNbKeys = root.getNbKeys();

		if (rootNbKeys > 1) {
			root.delete(key);//The minimal nbKeys for nodes other than the root is minDegree-1,while for the root it's 1

		} else {
			// The root has 1 key
			int[] rootKeys = root.getKeys(); 
			BtreeNode[] rootChildren = root.getChildren();
			int i = 0;

			while (i < rootNbKeys && rootKeys[i] < key) {
				i++;
			}
			if (rootKeys[0] == key) {//the key is in the root
				if (rootChildren[0].getNbKeys()==minDegree-1 && rootChildren[1].getNbKeys()==minDegree-1) {
					//merge and decrease the height and recurse
					root.mergeTwoChildren(0);
					System.out.println("Decreasing the height of the Btree");
					root = rootChildren[0];
					root.delete(key);

				} else if (rootChildren[0].getNbKeys()>=minDegree) {
					//find the predecessor, replace it in the root
					System.out.println("Deleting from the root, finding the predecessor...");
					rootKeys[0] = rootChildren[0].deletePredecessor();

				} else {
					//find the successor, replace it in the root
					System.out.println("Deleting from the root, finding the successor...");
					rootKeys[0] = rootChildren[1].deleteSuccessor();

				}

			} else {
				if (rootChildren[i].getNbKeys() >= minDegree) {
					rootChildren[i].delete(key);
				} else {
					if (rootChildren[1-i].getNbKeys() == minDegree-1) {
						//merge and decrease the height and recurse
						root.mergeTwoChildren(0);
						System.out.println("--Decreasing the height of the Btree");
						this.root = rootChildren[0];
						this.traverseHorizon();
						this.delete(key); // we don't call delete on a node but on the whole Btree
					} else {
						//borrow and recurse
						if (i==0) {
							root.borrowKeyFromRightChild(i);
						} else {
							root.borrowKeyFromLeftChild(i);
						}
						rootChildren[i].delete(key);
					}
				}
			}
			
		}
	}
	public void traverse() {
		this.root.traverse();
	}

	public void traverseHorizon() {
		System.out.println("\nBegin traverse Horizon");
		LinkedList<BtreeNode> toVisit = new LinkedList<BtreeNode>();
		toVisit.add(this.root);
		int level = -1; 
		BtreeNode firstNodeNextLevel = this.root;

		while (toVisit.size() > 0) {
			BtreeNode currentNode = toVisit.get(0);
			toVisit.remove(0);
			if (firstNodeNextLevel == currentNode) {
				level++;
				System.out.println("----level "+level);
				if (!currentNode.isLeaf()) {
					firstNodeNextLevel = currentNode.getChildren()[0];
				}
			}
			for (int i=0; i<currentNode.getNbKeys(); i++) {
				System.out.print(currentNode.getKeys()[i]+"-");
				if (!currentNode.isLeaf()) {
					toVisit.add(currentNode.getChildren()[i]);
				}
			}
			System.out.println();
			if (!currentNode.isLeaf()) {
				toVisit.add(currentNode.getChildren()[currentNode.getNbKeys()]);
			}
		}
		System.out.println("End traverse Horizon\n");
	}
}


class BtreeNode {

	private int nbKeys;
	private int[] keys;
	private int minDegree; //minimum degree = minimum number of children
	private BtreeNode[] children; //set only if the node is a non-leaf
	private boolean isLeaf;

	public BtreeNode(int minDeg, boolean leaf) {
		this.minDegree = minDeg;
		this.nbKeys = 0;
		this.keys = new int[2*minDegree-1];
		if (! leaf) {
			this.children = new BtreeNode[2*minDegree];
		}
		this.isLeaf = leaf;
	}

	public NodeAndIndex search(int key) {
		// @return (node, index) if the key exists, null otherwise
		// index is the index of the searched key in its node
		int i = 0;
		while (i < nbKeys && key > keys[i]) {
			i++;
		}
		if (i < nbKeys && key == keys[i]) {
			return new NodeAndIndex(this, i);
		}
		if (this.isLeaf) {
			return null;
		} else {
			return children[i].search(key);
		}
	}

	public void splitChild(int index) {
		/* splits the child children[index] */
		//Precondition : children[index] is a full node and this is not full (top down split)
		BtreeNode leftChild = this.children[index];
		BtreeNode rightChild = new BtreeNode(minDegree, leftChild.isLeaf()); // All leaves have the same depth
		int[] upperKeys = rightChild.getKeys();
		int[] lowerKeys = leftChild.getKeys();

		// copying minDegree-1 upper keys
		for (int i=0; i<minDegree-1; i++) {
			upperKeys[i] = lowerKeys[i+minDegree];
		}

		//copying minDegree upper children
		if (! leftChild.isLeaf()) {
			BtreeNode[] lowerChildren = leftChild.getChildren();
			BtreeNode[] upperChildren = rightChild.getChildren();
			for (int i=0; i<minDegree; i++) {
				upperChildren[i] = lowerChildren[i+minDegree];
			}
		}

		//shifting the upper children of the parent
		//the pointer to the right child will be in index+1 so we shift from index+1
		for (int i=this.nbKeys+1; i>index+1; i--) {
			this.children[i] = this.children[i-1];
		}

		//shifting the upper keys of the parent
		//the promoted key will be in index so we shift from index
		for (int i=this.nbKeys; i>index; i--) {
			this.keys[i] = this.keys[i-1];
		}

		this.keys[index] = lowerKeys[minDegree-1];
		this.children[index+1] = rightChild;

		// Updating number of keys for these 3 nodes
		leftChild.setNbKeys(minDegree-1); // Because of the nbKeys constraint, we can keep the upper keys
											// in the left child. Just like RAM contains in each cell sthg
											// even if it is free
		rightChild.setNbKeys(minDegree-1);
		this.nbKeys++;
	}

	public void insertNonFull(int key) {
		//Precondition : The current node is non full
		int index = this.nbKeys-1;
		if (this.isLeaf) {
			//shifting keys only
			while (index >= 0 && this.keys[index] > key) {
				this.keys[index+1] = this.keys[index];
				index--;
			}
			this.keys[index+1] = key;
			this.nbKeys++;
		} else {
			while (index >= 0 && this.keys[index] > key) {
				index--;
			}
			index++;
			if (this.children[index].nbKeys == 2*minDegree-1) {
				this.splitChild(index);
				if (this.keys[index] < key) { //this.keys[index] is the promoted key
					index++;
				}
			}
			this.children[index].insertNonFull(key);
		}
	}

	public void delete(int key) {
		// top-down delete, without doing a backward pass after this forward pass to preserve the Btree properties
		// All preservations are done forwardly in the first pass
		//Precondition : The current node has at least minDegree keys or it is the root having at least 2 keys
		int i = 0;
		while (i < this.nbKeys && this.keys[i]<key) {
			i++;
		}
		if (this.isLeaf) {
			if (i < this.nbKeys && this.keys[i]==key) { //if the key exists
				while (i < this.nbKeys-1) {
					this.keys[i] = this.keys[i+1];
					i++;
				}
				this.nbKeys--;
			}

		} else {
			if (i < this.nbKeys && this.keys[i]==key) { //exists in the current node
				if (this.children[i].getNbKeys() >= minDegree) {
					this.keys[i] = this.children[i].deletePredecessor();
				} else if (this.children[i+1].getNbKeys() >= minDegree) {
					this.keys[i] = this.children[i+1].deleteSuccessor(); 
				} else { //merge these children and keys[i]
					this.mergeTwoChildren(i);
					this.children[i].delete(key); //recursively delete from this child (we can't delete here because
						// the last pointer that was in the left child and the first pointer that was the right child 
						// will have no key between them)
				}
			} else { //doesn't exist in the current node
				int previousNbKeys = this.nbKeys;
				if (this.children[i].getNbKeys() == minDegree-1) {//ensuring the precondition above
					ensureDeletePrecondition(i);
					if (previousNbKeys > this.nbKeys) {
						i--;
					}
				}
				this.children[i].delete(key);
			}
		}
	}

	public int deletePredecessor() {
		/* delete the most right key in this subtree*/
		/* @return the deleted key */
		if (this.isLeaf) {
			nbKeys--;
			return this.keys[nbKeys];
		} else {
			return this.children[nbKeys].deletePredecessor();
		}
	}

	public int deleteSuccessor() {
		/* delete the most left key in this subtree*/
		/* @return the deleted key */
		if (this.isLeaf) {
			int res = this.keys[0];
			for (int i=0; i<nbKeys-1; i++) {
				this.keys[i] = this.keys[i+1];
			}
			this.nbKeys--;
			return res;
		} else {
			return this.children[0].deleteSuccessor();
		}
	}

	public void ensureDeletePrecondition(int i) {
		/* ensures that children[i] has at least minDegree keys*/
		//The node has the minimal number of keys
		//we borrow a key from one of the siblings of i th child if it has at least minDegree keys
		//If not we merge these 2
		if (i > 0 && this.children[i-1].getNbKeys()>=minDegree) { //the left sibling is suitable
			this.borrowKeyFromLeftChild(i);
		}
		else if (i < this.nbKeys && this.children[i+1].getNbKeys()>minDegree-1) { //the right suits
			this.borrowKeyFromRightChild(i);
		} else { //merge with the sibling
			if (i>0) {
				i--; //we will merge in the left child and drop the right
			}
			this.mergeTwoChildren(i);
		}
	}

	public void mergeTwoChildren(int i) {
		/* merge children[i] and children[i+1] */
		/* Precondition : They are both half full */
		if (children[i].getNbKeys()!=minDegree-1 || children[i+1].getNbKeys()!=minDegree-1) {
			throw new IllegalStateException("Precondition violeted");
		}
		this.children[i].getKeys()[minDegree-1] = this.keys[i]; //This is now the median of the merged node
		for (int j=0; j<minDegree-1; j++) {
			if (! this.children[i].isLeaf()) {
				this.children[i].getChildren()[j+minDegree] = this.children[i+1].getChildren()[j];
			}
			this.children[i].getKeys()[j+minDegree] = this.children[i+1].getKeys()[j];
		}
		if (! this.children[i].isLeaf()) {
			this.children[i].getChildren()[2*minDegree-1] = this.children[i+1].getChildren()[minDegree-1];
		}
		//debug
		// System.out.print("merging "+this+" [");
		// for (int l=0; l < nbKeys; l++) {
			// System.out.print(this.keys[l]+" , ");
		// }
		// System.out.println("]");

		// System.out.print("Setting "+this.children[i]+" [");
		// for (int l=0; l < 2*minDegree-1; l++) {
		// 	System.out.print(this.children[i].getKeys()[l]+" , ");
		// }
		// System.out.println("]");


		this.children[i].setNbKeys(2*minDegree-1);
		//removing the ith key and the i+1 child from the current node
		int index = i;
		while (index < this.nbKeys-1) {
			this.keys[index] = this.keys[index+1];
			this.children[index+1] = this.children[index+2];
			index++;
		}
		this.nbKeys--;
	}

	public void borrowKeyFromLeftChild(int i) {
		/* borrows from children[i-1] to children[i]*/
		int tmp = this.keys[i-1]; 
		this.keys[i-1] = this.children[i-1].getLastKey();
		this.children[i].rightShift(0);
		this.children[i].getKeys()[0] = tmp;
		this.children[i].getChildren()[0] = this.children[i-1].getLastChild();
		this.children[i-1].decrementNbKeys(); //decrement nb of keys
	}

	public void borrowKeyFromRightChild(int i) {
		/* borrows from children[i+1] to children[i]*/
		boolean childrenAreLeaves = this.children[i+1].isLeaf();
		BtreeNode firstChildRightSibling = null;

		int tmp = this.keys[i];
		this.keys[i] = this.children[i+1].getFirstKey();
		if (!childrenAreLeaves) {
			firstChildRightSibling = this.children[i+1].getFirstChild(); 
		}
		this.children[i+1].leftShift();
		this.children[i].getKeys()[minDegree] = tmp;
		if (!childrenAreLeaves) {
			this.children[i].getChildren()[minDegree+1] = firstChildRightSibling;
		}
		this.children[i].incrementNbKeys();
	}



	public void traverse() {
		for (int i=0; i<this.nbKeys; i++) {
			if (!this.isLeaf) {
				this.children[i].traverse();
			}
			System.out.print(this.keys[i]+" - ");
		}
		if (!this.isLeaf) {
			// System.out.println("traverse : "+this.nbKeys+" "+this);
			this.children[this.nbKeys].traverse();
		}
	}


	public int[] getKeys() {
		return this.keys;
	}

	public int getNbKeys() {
		return this.nbKeys;
	}

	public void setNbKeys(int nb) {
		this.nbKeys = nb;
	}

	public BtreeNode[] getChildren() {
		return this.children;
	}

	public boolean isLeaf() {
		return isLeaf;
	}

	public int getLastKey() {
		return this.keys[nbKeys-1];
	}

	public int getFirstKey() {
		return this.keys[0];
	}

	public BtreeNode getLastChild() {
		return this.children[nbKeys];
	}

	public BtreeNode getFirstChild() {
		return this.children[0];
	}

	public void rightShift(int start) {
		//right shift KEYS AND CHILDREN from start AND INCREMENT nbKeys
		int i = nbKeys;
		while (i > start) {
			keys[i] = keys[i-1];
			if (! this.isLeaf()) {
				children[i+1] = children[i];
			}
			i--;
		}
		if (! this.isLeaf()) {
			children[start+1] = children[start];
		}
		nbKeys++;
	}

	public void leftShift() {
		// left shift KEYS AND CHILDREN from end & DECREMENT nbKeys 
		int i = 0;
		while (i < nbKeys-1) {
			keys[i] = keys[i+1];
			if (! this.isLeaf()) {
				children[i] = children[i+1];
			}
			i++;
		}
		if (!this.isLeaf()) {
			children[nbKeys-1] = children[nbKeys];
		}
		nbKeys--;
	}

	public void incrementNbKeys() {
		this.nbKeys++;
	}

	public void decrementNbKeys() {
		this.nbKeys--;
	}
}

class NodeAndIndex {
	private BtreeNode node;
	private int index;

	public NodeAndIndex(BtreeNode node, int index) {
		this.node = node;
		this.index = index;
	}
}
