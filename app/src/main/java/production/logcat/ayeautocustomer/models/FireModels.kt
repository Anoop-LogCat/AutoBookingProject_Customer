package production.logcat.ayeautocustomer.models

data class FireStandModel(val standName:String,val testMode:Boolean,val landMark:String,val latitude:Number,val longitude:Number)

data class FireDriverModel(val age:String,val driverLat:Number,val driverLong:Number, val autoNumber:String, val workingTime:String, val imageUrl:String,val phone:String,val username:String)

data class FireCustomerModel(var imageUrl:String, var phone:String, var username:String,val token:String, var nominee:MutableMap<String,Number>,var history:MutableMap<String,String>)