# brouter-android-lib
Attempting to provide a library wrapper to integrate [BRouter](https://github.com/abrensch/brouter/) into an Android app

## Usage

Initialise the library to ensure the folder structure with the app and profiles are extracted
```
val dir = context.getExternalFilesDir(null)
BRouter.initialise(context, dir.toString())
```
You must manage the segment files as required
```
val segmentDir = BRouter.segmentsFolderPath(dir)
// copy segment files to this directory
```
Use the Builder to create the parameters class and generate a route
```
val params = RoutingParams.Builder(dir.toString())
                .profile(Profile.TREKKING)
                .from(54.543592, -2.950076)
                .addVia(54.530371, -3.004975)
                .to(54.542671, -2.966995)
                .build()

val track = BRouter.generateRoute(params)
```        
