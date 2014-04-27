
public class Loader {

	int mCurrentIndex = 0;
	PacketTimeline mPacketTimeline;
	TruckDataList mTruckDatalist;
	int numberOfTrucks = 0;

	public Loader(){}
	
	public TruckDataUnit next(){
//		if(mPacketTimeline.mTimeline.size() == 5000){
//			System.out.println("reached 5000");
//		}
		int timeLineSize = mPacketTimeline.mTimeline.size();
//		if(mCurrentIndex < mPacketTimeline.mTimeline.size() && mPacketTimeline.mTimeline.size() > 5000){
		if(timeLineSize > 5000){

		TruckDataUnit truckDataUnit = mPacketTimeline.mTimeline.get(mCurrentIndex);
		mTruckDatalist.truckDataList.set(truckDataUnit.mNumber, truckDataUnit);
		
		}
		System.out.println("CurrentIndex: "+mCurrentIndex +" timelineSize: "+timeLineSize);
		 
		 //;
		 //);
//		 mCurrentIndex++;
		 //return truckDataUnit;
//		}
		return null;
	}
	
	public void setIndex(int index){
		mCurrentIndex = index;
	}
	
	public void setTimeline(PacketTimeline timeline){
		mPacketTimeline = timeline;
	}
	
	public void setTruckDataList(TruckDataList truckDataList){
		mTruckDatalist = truckDataList;
	}
}
