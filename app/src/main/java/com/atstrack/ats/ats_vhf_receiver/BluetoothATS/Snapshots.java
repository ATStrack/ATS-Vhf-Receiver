package com.atstrack.ats.ats_vhf_receiver.BluetoothATS;

import android.os.Parcel;
import android.os.Parcelable;

import com.atstrack.ats.ats_vhf_receiver.Utils.Converters;

import java.util.Calendar;

/**
 *
 */
public class Snapshots implements Parcelable {
    private final static String TAG = Snapshots.class.getSimpleName();

    public static final Creator<Snapshots> CREATOR = new Creator<Snapshots>() {
        @Override
        public Snapshots createFromParcel(Parcel in) {
            return new Snapshots(in);
        }

        @Override
        public Snapshots[] newArray(int size) {
            return new Snapshots[size];
        }
    };
    private String fileName;
    private boolean error;
    private boolean filled;
    private byte[] snapshot;
    public int byteIndex;
    private int packIndex;
    private boolean date;
    private boolean end;
    private int YYYY;
    private int MM;
    private int DD;
    private int mss;
    private String macNumber;
    private int size;
    public int outOfRange;

    public Snapshots(){
        //attributable variables
        snapshot = new byte[1024]; //16402 2048
        fileName = "";
        macNumber = "";
        //internal use variables
        byteIndex = 0;
        packIndex = 0;
        filled = false;
        error = false;
        date = false;
        end = false;
        size = 1024;
        outOfRange = 0;
    }

    public Snapshots(int size){
        //attributable variables
        snapshot = new byte[size];
        fileName = "";
        macNumber = "";
        //internal use variables
        byteIndex = 0;
        packIndex = 0;
        filled = false;
        error = false;
        date = false;
        end = false;
        this.size = size;
        outOfRange = 0;
    }

    private Snapshots(Parcel in) {
        fileName = in.readString();
        filled = in.readByte() != 0;
        error = in.readByte() != 0;
        snapshot = in.createByteArray();
        byteIndex = in.readInt();
        packIndex = in.readInt();
        date = in.readByte() != 0;
        end = in.readByte() != 0;
        YYYY = in.readInt();
        MM = in.readInt();
        DD = in.readInt();
        mss = in.readInt();
    }

    public String getFileName() {
        return fileName;
    }

    private void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public byte[] getSnapshot() {
        return snapshot;
    }

    public boolean isFilled() {
        return filled;
    }

    public boolean isError() {
        return error;
    }

    public int getPackIndex() {
        return packIndex;
    }

    public void setMacNumber(String macNumber) { this.macNumber = macNumber; }

    public String getMacNumber() { return macNumber; }

    private void setFileNameRaw(){
        fileName = "";
        Calendar time = Calendar.getInstance();
        fileName = "D_" + (((time.get(Calendar.MONTH) + 1)<10)? "0" + (time.get(Calendar.MONTH) + 1): time.get(Calendar.MONTH) + 1)
                + "_" + ((time.get(Calendar.DAY_OF_MONTH)<10)? "0" + time.get(Calendar.DAY_OF_MONTH): time.get(Calendar.DAY_OF_MONTH))
                + "_" + time.get(Calendar.YEAR)
                + "_" + ((time.get(Calendar.HOUR_OF_DAY)<10)? "0" + time.get(Calendar.HOUR_OF_DAY): time.get(Calendar.HOUR_OF_DAY))
                + "_" + ((time.get(Calendar.MINUTE)<10)? "0" + time.get(Calendar.MINUTE): time.get(Calendar.MINUTE))
                + "_" + ((time.get(Calendar.SECOND)<10)? "0" + time.get(Calendar.SECOND): time.get(Calendar.SECOND)) + "Raw" + ".txt";
    }

    private void setFileNameDownload(){
        fileName = "";
        Calendar time = Calendar.getInstance();
        fileName = "D_" + (((time.get(Calendar.MONTH) + 1)<10)? "0" + (time.get(Calendar.MONTH) + 1): time.get(Calendar.MONTH) + 1)
                + "_" + ((time.get(Calendar.DAY_OF_MONTH)<10)? "0" + time.get(Calendar.DAY_OF_MONTH): time.get(Calendar.DAY_OF_MONTH))
                + "_" + time.get(Calendar.YEAR)
                + "_" + ((time.get(Calendar.HOUR_OF_DAY)<10)? "0" + time.get(Calendar.HOUR_OF_DAY): time.get(Calendar.HOUR_OF_DAY))
                + "_" + ((time.get(Calendar.MINUTE)<10)? "0" + time.get(Calendar.MINUTE): time.get(Calendar.MINUTE))
                + "_" + ((time.get(Calendar.SECOND)<10)? "0" + time.get(Calendar.SECOND): time.get(Calendar.SECOND)) + ".txt";
    }

    private void setFileNameDisplay(){
        fileName = "";
        Calendar time = Calendar.getInstance();
        fileName = "DisplayLog_" + (((time.get(Calendar.MONTH) + 1)<10)? "0" + (time.get(Calendar.MONTH) + 1): time.get(Calendar.MONTH) + 1)
                + "_" + ((time.get(Calendar.DAY_OF_MONTH)<10)? "0" + time.get(Calendar.DAY_OF_MONTH): time.get(Calendar.DAY_OF_MONTH))
                + "_" + time.get(Calendar.YEAR)
                + "_" + ((time.get(Calendar.HOUR_OF_DAY)<10)? "0" + time.get(Calendar.HOUR_OF_DAY): time.get(Calendar.HOUR_OF_DAY))
                + "_" + ((time.get(Calendar.MINUTE)<10)? "0" + time.get(Calendar.MINUTE): time.get(Calendar.MINUTE))
                + "_" + ((time.get(Calendar.SECOND)<10)? "0" + time.get(Calendar.SECOND): time.get(Calendar.SECOND)) + ".txt";
    }

    public void processSnapshot(byte[] packRead){
        try {
            if (packIndex < 9) {
                if (packIndex == 0)
                    setFileNameRaw();
                if ((packIndex%8)>0 || packIndex == 0) {
                    System.arraycopy(packRead, 0, snapshot, byteIndex, packRead.length);
                    byteIndex = byteIndex + 244;
                } else {
                    System.arraycopy(packRead, 0, snapshot, byteIndex, packRead.length);
                    byteIndex = byteIndex + 96;
                    packIndex = -1;
                }
                packIndex++;
                if ((byteIndex == size)) filled = true;
            } else {
                if (filled)
                    throw new IndexOutOfBoundsException("Index out of range, this byte doesn't exist.");
                else
                    throw new Exception("Unknown error while processing a snapshot." + " || bI=" + byteIndex + ", pI="+ packIndex);
            }
        }
        catch (Exception e) {
            setFileName(getFileName() + " || error: " + e.getMessage());
            error = true;
        }
    }

    public void processSnapshotDownload(byte[] packRead){
        try{
            if (packRead.length>0) {
                if (packIndex == 0)
                    setFileNameDownload();
                System.arraycopy(packRead, 0, snapshot, byteIndex, packRead.length);
                byteIndex += packRead.length;
            }
        }
        catch (Exception e) {
            setFileName(getFileName() + " || error: " + e.getMessage());
            error = true;
        }
    }

    public void processSnapshotDisplay(byte[] packRead){
        try {
            if (packRead.length>0) {
                if (packIndex == 0)
                    setFileNameDisplay();
                if (packRead.length + byteIndex > size){
                    outOfRange = size - byteIndex;
                    System.arraycopy(packRead, 0, snapshot, byteIndex, size - byteIndex);
                    packIndex++;
                    byteIndex += (size - byteIndex);
                }else{
                    System.arraycopy(packRead, 0, snapshot, byteIndex, packRead.length);
                    byteIndex += packRead.length;
                    packIndex++;
                }
                if (byteIndex == size) filled = true;
            }
        }
        catch (Exception e) {
            setFileName(getFileName() + " || error: " + e.getMessage());
            error = true;
        }
    }




    /**
     * <h1>setFileName</h1>
     * Generates date and time from a byte array, setting variable fileName with the results.
     * Uses global variables to fill fileName.
     */
    private void setFileNameM(){
        fileName = "";
        //get date and build new calendar
        Calendar snapTime = Calendar.getInstance();
        //Get YEAR, MONTH and DAY_OF_MONTH and set new Calendar
        snapTime.set(Calendar.HOUR_OF_DAY,0);
        snapTime.set(Calendar.MINUTE,0);
        snapTime.set(Calendar.SECOND,0);
        snapTime.set(Calendar.MILLISECOND,0);
        snapTime.set(YYYY, MM, DD);
        //Get MILLISECONDS OF DAY and ADD to new Calendar
        snapTime.add(Calendar.MILLISECOND,mss);
        //write strings to return
        fileName = "G_" + YYYY
                + "_" + ((MM+1<10)? "0"+(MM+1) : (MM+1))
                + "_" + ((DD<10)? "0"+DD : DD)
                + "_" + ((snapTime.get(Calendar.HOUR_OF_DAY)<10)? "0"+snapTime.get(Calendar.HOUR_OF_DAY) : snapTime.get(Calendar.HOUR_OF_DAY))
                + "_" + ((snapTime.get(Calendar.MINUTE)<10)? "0"+snapTime.get(Calendar.MINUTE) : snapTime.get(Calendar.MINUTE))
                + "_" + ((snapTime.get(Calendar.SECOND)<10)? "0"+ snapTime.get(Calendar.SECOND): snapTime.get(Calendar.SECOND))
                // + "_" + ((snapTime.get(Calendar.MILLISECOND)<10)?"00"+snapTime.get(Calendar.MILLISECOND) : (snapTime.get(Calendar.MILLISECOND)<100)?"0"+snapTime.get(Calendar.MILLISECOND) : snapTime.get(Calendar.MILLISECOND)) + ".ors";
                + "_"+ getMacNumber() +".ors";
    }

    /**
     * <h1>processSnapshot</h1>
     * GET PACKAGES WRITTEN IN THE ORS FORMAT
     * @param packRead Conceived to receive 244 bytes.
     */
    public void processSnapshotM(byte[] packRead){
        try {
            //Try to extract a date from the first 8 bytes
            byte[] snapDate = {packRead[0], packRead[1], packRead[2], packRead[3], packRead[4], packRead[5]
                    , packRead[6], packRead[7]};
            byte[] macByte = {packRead[13], packRead[12], packRead[11], packRead[10], packRead[9], packRead[8]};
            //Are those DATE or END?
            if (packIndex==0) {
                if (!(date)) date = isRealDate(snapDate);
                if (!(end)) end = isTheEnd(snapDate);
                //Begin the filling or finishing
                if (end) {//this is the end of all files
                    setFileName("EoF");
                    filled = true;
                }
                else {//if (date) {//this is the first Packet.
                    // Header (starting in byte 0)
                    byte[] header = {(byte) 0x00, (byte) 0x80, (byte) 0x12, (byte) 0x00, (byte) 0xEF
                            , (byte) 0xBE, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00};
                    setMacNumber(Converters.getHexValue(macByte).replace(" ",""));
                    setFileNameM();
                    //header of new snapshot
                    System.arraycopy(header, 0, snapshot, 0, header.length);
                    //date of new snapshot
                    System.arraycopy(snapDate, 0, snapshot, header.length, snapDate.length);
                    //update byteIndex and packRead
                    byteIndex = byteIndex + header.length +snapDate.length;//18
                    packIndex++;
                    //count if error
                    if(!(date)&&!(end)) error = true;
                }
            }
            else if ((packIndex > 0)&&(packIndex < 73)) {//this is your average Packet (14) 73
                if ((packIndex%9)>0) { //9
                    //src.length=244 srcPos=0 dst.length=19114 dstPos=bI length=244
                    System.arraycopy(packRead, 0, snapshot, byteIndex, packRead.length);
                    byteIndex = byteIndex + 244;
                } else {
                    //src.length=244 srcPos=0 dst.length=19114 dstPos=bI length=144
                    System.arraycopy(packRead, 0, snapshot, byteIndex, 96);
                    byteIndex = byteIndex + 96;
                }
                packIndex++;
                if ((byteIndex == 16402)&&(packIndex==73)) filled = true; //16402 73
            }
            else {
                if (filled)
                    throw new IndexOutOfBoundsException("Index out of range, this byte doesn't exist.");
                else
                    throw new Exception("Unknown error while processing a snapshot." + " || bI=" + byteIndex + ", pI="+ packIndex);
            }
        }
        catch (Exception e) {
            setFileName(getFileName() + " || error: " + e.getMessage());
            error = true;
        }
    }

    /**
     * <h1>isRealDate</h1>
     * This function confirm or negates if a bytes array is a date according the .ors file format.
     * @param bytesFromSnap a byte array coming from a snapshot package being processed.
     * @return true: If it resemble a date.
     *
     */
    private boolean isRealDate(byte[] bytesFromSnap){
        boolean date = false;
        Calendar today = Calendar.getInstance();
        YYYY = 1900; mss = 0;
        //get gate and set new calendar
        //set another calendar w today's date; set yet another one, copy the last one but reduce 3 months
        //if the array falls in between, return true; if not, false
        for (int i = 0; i < 8; i++) {
            int b = Integer.parseInt(Converters.getDecimalValue(bytesFromSnap[i]));
            switch (i){
                case 0:
                    YYYY = YYYY + b;
                    break;
                case 1:
                    YYYY = YYYY + (b * 256);
                    if ((Integer.compare(YYYY, today.get(Calendar.YEAR)) > 1)
                            || (Integer.compare(YYYY, today.get(Calendar.YEAR)) < (-1)))
                        i = 8;
                    break;
                case 2:
                    if ((b < 0) || (b >= 12))
                        i = 8;
                    else
                        MM = b;
                    break;
                case 3:
                    if ((b < 1) || (b >= 32))
                        i = 8;
                    else
                        DD = b;
                    break;
                case 4:
                    mss = b;
                    break;
                case 5:
                    mss = mss + (b * 256);
                    break;
                case 6:
                    mss = mss + (b * (int) Math.pow(256,2));
                    break;
                case 7:
                    mss = mss + (b * (int) Math.pow(256,3));
                    if (mss >= 86400000)
                        i = 8;
                    else
                        date = true;
                    break;
            }
        }
        return date;
    }

    /**
     * <h1>isTheEnd</h1>
     * SEE IF THE ARRAY IS THE END OF FILES
     * @param bytesFromSnap Conceived to receive 16 bytes.
     * @return true if it's EoF
     */
    private boolean isTheEnd(byte[] bytesFromSnap){//could be byte but careful careful
        //set static bytes from end, compare, if equal return true
        String compare = Converters.getHexValue(bytesFromSnap);
        compare = compare.replace(" ", "");
        return (compare.equals("ABCABCABCABCABCA"));
    }

    /**
     * Describe the kinds of special objects contained in this Parcelable
     * instance's marshaled representation. For example, if the object will
     * include a file descriptor in the output of {@link #writeToParcel(Parcel, int)},
     * the return value of this method must include the
     * {@link #CONTENTS_FILE_DESCRIPTOR} bit.
     *
     * @return a bitmask indicating the set of special object types marshaled
     * by this Parcelable object instance.
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Flatten this object in to a Parcel.
     *
     * @param dest  The Parcel in which the object should be written.
     * @param flags Additional flags about how the object should be written.
     *              May be 0 or {@link #PARCELABLE_WRITE_RETURN_VALUE}.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(macNumber);
        dest.writeString(fileName);
        dest.writeByte((byte) (filled ? 1 : 0));
        dest.writeByte((byte) (error ? 1 : 0));
        dest.writeByteArray(snapshot);
        dest.writeInt(byteIndex);
        dest.writeInt(packIndex);
        dest.writeByte((byte) (date ? 1 : 0));
        dest.writeByte((byte) (end ? 1 : 0));
        dest.writeInt(YYYY);
        dest.writeInt(MM);
        dest.writeInt(DD);
        dest.writeInt(mss);
    }
}
