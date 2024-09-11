
Asset Tracker Lite is an introductory asset tracking application designed for small businesses. It is a simple, license-free application for Zebra Android mobile computers and is the first step in improving asset visibility. Employees simply walk through a facility and scan the barcodes on the assets. It helps increase the value of Zebra mobile computers.
Asset tracking is fast, easy, and accurate, and so cost-effective. You can take asset inventory as often as business needs dictate.
Asset Tracker Lite improves asset visibility, ensuring that workers can locate the assets they need to get the job done. It also improves worker productivity, asset utilization, and asset value.


Asset Tracker Lite V2.0

1. Added support for Android 13.

2. Application file access location in the device has been moved from external storage to /data/tmp/public.

3. Saving username and password for FTP server on settings.xml is disabled.

4. Added support for FTPS server uploading instead of FTP uploading.

5. Added support for product database import on “xml” format.

6. Added support for product database import on “csv” format.

7. Removed Webdav server uploading support.



**ChangeLogs : 01/11/2017**
1. Pop-up upon FTP upload completion/failure.
2. Added id's for UI elements for testing.
3. Bug-fixed in AddNewInventory: clicking Save Button crashed the application.

**ChangeLogs : 12/14/2017**
1. Added Settings to enable or disable add to DB mode


**ChangeLogs : 11/17/2017**

**AddNewInventory** Activity can be launched in two ways: 

1. If a new Item which doesn't exist in DB is scanned

2. From Drop down menu on top right of MainActivity

##

* In case 1: User and Site will be read from *MainActivity*

* In case 2: if not present the default values for **User** and **Site** are *"Admin"* & *"testBench"* resp.


##

* As of now it reads new items and creates a csv file which is ready to be uploaded to DB.
Upload to DB part is a available but "commented out" for testing purposes.

* All the items added to the list are saved to csv/xml file depending upon preferences on back press
in AddNewInventory Screen.

* Cancel removes whatever data was on screen in case wrong data is entered.

* Add button is disabled on *AddNewInventory* unless there is data in Value, Barcode and Price.