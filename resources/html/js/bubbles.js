
/*
Part of Bubbles Engine, 2012 by J.Ramb
*/

(function() {
  var $;

  $ = jQuery;

  if (typeof bubbles === "undefined" || bubbles === null) bubbles = {};

  $.ajaxSetup({
    error: function(data, textStatus, jqXHR) {
      alert("" + textStatus);
      return alert("Resp: " + data.responseText);
    },
    timeout: 10000
  });

  bubbles.getlocation = function(silent) {
    var fail, success;
    if (navigator.geolocation != null) {
      success = function(position) {
        if (!(silent != null)) bubbles.toast("Location retrieved");
        return bubbles.location = {
          timestamp: position.timestamp,
          coords: {
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
            accuracy: position.coords.accuracy
          }
        };
      };
      fail = function(error) {
        return bubbles.location = (function() {
          switch (error.code) {
            case error.TIMEOUT:
              return "Timeout";
            case error.POSITION_UNAVAILABLE:
              return "Position unavailable";
            case error.PERMISSION_DENIED:
              return "Permission denied";
            case error.UNKNOWN_ERROR:
              return "Unknown error";
            default:
              return "???";
          }
        })();
      };
      navigator.geolocation.getCurrentPosition(success, fail, {
        timeout: 10000
      });
    }
    return;
  };

  bubbles.toast = function(msg) {
    return $("<div class='ui-loader ui-overlay-shadow ui-body-e ui-corner-all'><h1>" + msg + "</h1></div>").css({
      "display": "block",
      "opacity": 0.96,
      "top": $(window).scrollTop() + 100
    }).appendTo($.mobile.pageContainer).delay(1500).fadeOut(400, function() {
      return $(this).remove();
    });
  };

  bubbles.rpc = function(target, method, pform, whenOk) {
    var name, params, value, _i, _len, _ref, _ref2;
    if (pform) {
      params = {
        location: bubbles.location
      };
      _ref = $(pform).serializeArray();
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        _ref2 = _ref[_i], name = _ref2.name, value = _ref2.value;
        params[name] = value;
      }
      $.ajax({
        type: "POST",
        contentType: "application/json; charset=UTF-8",
        url: "" + target + "/rpc",
        beforeSend: function() {
          return $.mobile.showPageLoadingMsg();
        },
        data: JSON.stringify({
          "jsonrpc": "2.0",
          "method": method,
          "params": params,
          "id": 1
        }),
        success: function(data) {
          var _base;
          $.mobile.hidePageLoadingMsg();
          if (data.error) {
            return bubbles.toast((typeof (_base = data.error).data === "function" ? _base.data("" + data.error.message + ": " + data.error.data) : void 0) ? void 0 : data.error.message);
          } else {
            if (whenOk) return $.mobile.changePage(whenOk);
          }
        },
        dataType: "json"
      });
      return;
    }
  };

  bubbles.setident = function(newid) {
    return bubbles.ident = newid;
  };

  bubbles.pwlogin = function(redir, domain) {
    var data, name, value, _i, _len, _ref, _ref2;
    data = "domain=" + domain;
    _ref = $("#passwordForm").serializeArray();
    for (_i = 0, _len = _ref.length; _i < _len; _i++) {
      _ref2 = _ref[_i], name = _ref2.name, value = _ref2.value;
      data += "&" + name + "=" + value;
    }
    $.ajax({
      type: "POST",
      url: "/login",
      accept: "application/json; charset=UTF-8",
      data: data,
      success: function(data, textStatus, jqXHR) {
        if (data.status === "okay" && (data.name != null)) {
          bubbles.ident = data.name;
          return window.location.href = redir;
        } else {
          bubbles.ident = null;
          return bubbles.toast("Login failed!");
        }
      },
      dataType: "json"
    });
    return;
  };

  bubbles.login = function(redir) {
    var verifunction;
    verifunction = function(assertion) {
      var wl;
      wl = window.location;
      return $.ajax({
        type: "POST",
        url: "/login",
        accept: "application/json; charset=UTF-8",
        data: "assertion=" + assertion + "&audience=" + wl.protocol + "//" + wl.host,
        success: function(data, textStatus, jqXHR) {
          if (data.status === "okay" && (data.name != null)) {
            bubbles.ident = data.name;
          } else {
            bubbles.ident = null;
            bubbles.toast("Not logged in!");
          }
          if (assertion) {
            return window.location.href = redir;
          } else {
            return location.reload();
          }
        },
        dataType: "json"
      });
    };
    if (bubbles.ident != null) {
      if (confirm("You are logged in as " + bubbles.ident + ". Log out?")) {
        bubbles.ident = null;
        bubbles.toast("Logged out!");
        verifunction(null);
      }
    } else {
      navigator.id.get(verifunction);
    }
    return;
  };

  bubbles.gologin = function(domain) {
    if (bubbles.ident != null) {
      bubbles.login(null);
    } else {
      window.location.href = "/login?domain=" + domain + "&return=" + window.location;
    }
    return;
  };

  bubbles.thispage = function() {
    return window.location.href = "https://chart.googleapis.com/chart?chs=320x320&choe=UTF-8&cht=qr&chl=" + window.location;
  };

  $(function() {
    return bubbles.getlocation(true);
  });

}).call(this);
