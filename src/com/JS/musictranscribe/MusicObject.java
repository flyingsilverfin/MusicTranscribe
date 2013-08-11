public abstract class MusicObject {
	private int mLength;
	private int mReference;
	

	public int getLength() {
		return mLength;
	}

	public int getReference() {
		return mReference;
	}

	public void setLength(int len) {
		mLength = len;
	}
	
	public void setReference(int ref) {
		mReference = ref;
	}

	public abstract String getName();
}
