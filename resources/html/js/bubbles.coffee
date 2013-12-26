###
Part of Bubbles Engine, 2012 by J.Ramb
###
#compile: coffee -c bubbles.coffee
#develop: coffee -clw bubbles.coffee

$=jQuery # safest way to JQuery $
bubbles ?= {}


$.ajaxSetup(
        error: (data, textStatus, jqXHR) ->
                alert "#{textStatus}"
                alert "Resp: #{data.responseText}"
        timeout: 10000
        )

bubbles.getlocation = (silent) ->
    if navigator.geolocation?
      success=(position) ->
          #alert "Success: #{JSON.stringify position}"
          # for name, value of position.coords
          #         alert "#{name}=#{value}"
          if not silent?
            bubbles.toast "Location retrieved"
          bubbles.location=
                timestamp: position.timestamp
                coords:
                        latitude: position.coords.latitude
                        longitude: position.coords.longitude
                        accuracy: position.coords.accuracy
      fail= (error) ->
         bubbles.location = switch error.code
            when error.TIMEOUT then "Timeout"
            when error.POSITION_UNAVAILABLE then "Position unavailable"
            when error.PERMISSION_DENIED then "Permission denied"
            when error.UNKNOWN_ERROR then "Unknown error"
            else "???"
        #alert "Not so good: #{text}"
      navigator.geolocation.getCurrentPosition success, fail, {timeout:10000}
    undefined

bubbles.toast = (msg) ->
    $("<div class='ui-loader ui-overlay-shadow ui-body-e ui-corner-all'><h1>"+msg+"</h1></div>")
        .css( "display": "block", "opacity": 0.96, "top": $(window).scrollTop() + 100 )
        .appendTo( $.mobile.pageContainer )
        .delay( 1500 )
        .fadeOut 400, -> $(this).remove()

bubbles.rpc = (target, method, pform, whenOk) ->
    if pform
      params = {location: bubbles.location}
      for {name,value} in $(pform).serializeArray()
        # This will not work for multiple select!
        params[name]=value
      $.ajax(
        type: "POST"
        contentType: "application/json; charset=UTF-8"
        url: "#{target}/rpc"
        beforeSend: () -> #(xhr) ->
            # xhr.setRequestHeader("Authorization","Basic "+"YW50b246bmF2aWdhdGU=");
            $.mobile.showPageLoadingMsg()
        data: JSON.stringify {"jsonrpc":"2.0","method":method,"params":params,"id":1}
        #error: (data, textStatus, jqXHR) ->
        #    # debug: $.jramb = data;
        #    bubbles.toast "Error calling server: #{data.responseText}"
        success: (data) -> #data, textStatus, jqXHR
            # debug: $.jramb = data;
            # xhr.result=null if ok or xhr.error.message => error message
            $.mobile.hidePageLoadingMsg()
            if data.error
                bubbles.toast if data.error.data? "#{data.error.message}: #{data.error.data}" else data.error.message
            else
                if whenOk
                   $.mobile.changePage whenOk
        dataType: "json")
      #console.log "Nach ajax"
      undefined #return void(0)

bubbles.setident = (newid) ->
        bubbles.ident=newid


bubbles.pwlogin = (redir,domain) ->
        data="domain=#{domain}"
        for {name,value} in $("#passwordForm").serializeArray()
                data += "&#{name}=#{value}"
        $.ajax(
           type: "POST"
           url: "/login"
           accept: "application/json; charset=UTF-8"
           data: data
           success: (data, textStatus, jqXHR) ->
                   #alert "/login returned"
                   if data.status is "okay" and data.name?
                     bubbles.ident = data.name
                     #location.reload()
                     # $(".blogin").text bubbles.ident
                     # bubbles.toast "Welcome, #{bubbles.ident}!"
                     window.location.href=redir
                   else
                     bubbles.ident = null
                     bubbles.toast "Login failed!"
                     #location.reload()
                     #$(".blogin").text "Login",
           dataType: "json")
        undefined # necessary, since this is called from a button


bubbles.login = (redir) ->
        # this is the callback function for browser-id, it takes exactly one parameter
        verifunction = (assertion) ->
            #alert "Assertion is empty" if not assertion?
            wl=window.location
            $.ajax(
                type: "POST"
                url: "/login"
                accept: "application/json; charset=UTF-8"
                data: "assertion=#{assertion}&audience=#{wl.protocol}//#{wl.host}"
                #error: (data, textStatus, jqXHR) ->
                #        #alert "Error calling server: #{data.responseText}"
                #        # This happens...?!?
                #        if assertion
                #                window.location.href=redir
                #        else
                #                location.reload()
                success: (data, textStatus, jqXHR) ->
                   #alert "/login returned"
                   if data.status is "okay" and data.name? #was: email?
                     bubbles.ident = data.name #was email
                     #location.reload()
                     # $(".blogin").text bubbles.ident
                     # bubbles.toast "Welcome, #{bubbles.ident}!"
                   else
                     bubbles.ident = null
                     bubbles.toast "Not logged in!"
                     #location.reload()
                     #$(".blogin").text "Login",
                   if assertion
                        window.location.href=redir
                   else
                        location.reload()
                dataType: "json")

        if bubbles.ident?
                if confirm "You are logged in as #{bubbles.ident}. Log out?"
                        bubbles.ident = null
                        bubbles.toast "Logged out!"
                        #window.location.href=redir
                        #bubbles.ident=null
                        verifunction null
        else #if not bubbles.ident?
                # hot-load the browserid.org script, because it is slow..? $.getScript (no go with mobiles)?
                navigator.id.get verifunction
        undefined #


bubbles.gologin = (domain) ->
        if bubbles.ident?
                bubbles.login null
        else
                window.location.href="/login?domain=#{domain}&return=#{window.location}"
        undefined



bubbles.thispage = ->
    # we could maybe use "replace" here?
    window.location.href="https://chart.googleapis.com/chart?chs=320x320&choe=UTF-8&cht=qr&chl=#{window.location}"


$ ->
  #$(".blogin").click bubbles.login
  #alert(navigator.id.get)
  bubbles.getlocation(true)
