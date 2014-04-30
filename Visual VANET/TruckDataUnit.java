
public class TruckDataUnit {

	boolean mAlive;
	int mNumber;
	int mLocationX;
	String mPlatoonId;
	String mInformation;
	
	public TruckDataUnit(){
		mAlive = false;
		mNumber = -1;
		mLocationX = -1;
		mPlatoonId = "";
		mInformation = "";
	}
	
	void updateData(int number, int locationX, String platoonId, String information){
		mNumber = number;
		mLocationX = locationX;
		mPlatoonId = platoonId;
		mInformation = information;
		mAlive = true;
	}
	
}
