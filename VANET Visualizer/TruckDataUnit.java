
public class TruckDataUnit {

	boolean mAlive;
	int mNumber;
	int mLocationX;
	int mPlatoonId;
	String mInformation;
	
	public TruckDataUnit(){
		mAlive = false;
		mNumber = -1;
		mLocationX = -1;
		mPlatoonId = -1;
		mInformation = "";
	}
	
	void updateData(int number, int locationX, int platoonId, String information){
		mNumber = number;
		mLocationX = locationX;
		mPlatoonId = platoonId;
		mInformation = information;
		mAlive = true;
	}
	
}
