package com.reactnative.googlefit

import com.facebook.react.bridge.WritableMap

class Workout(map: WritableMap) {
   var startTime = map.getDouble("startTime")
    var endTime = map.getDouble("endTime")
    var id = map.getString("id")!!
    var name: String? = map.getString("name")!!
    var description: String? = map.getString("description")

}