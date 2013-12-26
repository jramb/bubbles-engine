$=jQuery
gotPing = (data) ->
  data.start_time = new Date(data.start_time)
  pingRenderer.render data
  window.setTimeout doPing, 5000

debugOn = false
pingRenderer = Tempo.prepare("buseinfo")
debug = (str) ->
  $("#debug").html str  if debugOn

doPing = ->
  $.ajax
    type: "GET"
    contentType: "application/json"
    url: "/ping"
    success: gotPing
    dataType: "json"

doPing()
