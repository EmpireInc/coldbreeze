define(['angular', 'app', 'controllers'], function (angular, minorityApp, minorityControllers) {

// For all processes
minorityControllers.controller('LaunchesCtrl', ['$q','$controller','$timeout','$http',
  '$window',
  '$translate',
  '$scope',
  '$filter',
  '$routeParams',
  '$rootScope',
  'Restangular',
  'ngDialog',
  'FilteredSessionsFactory',
  'InteractionsBulkFactory',
  'AllLaunchedElementsBulkFactory',
  'ProcPermissionsFactory',
  'TreeBuilder',
  'BPStationsFactory',
  'SessionsFactory',
  'BProcessesFactory',
  'BProcessFactory',
  'ObserversFactory',
  'ObserverFactory',
  'BProcessesFactory',
  'BPSpacesFactory',
  'BPSpaceElemsFactory','BPStationsFactory','BPStationFactory', 'BPLogsFactory', '$location', '$route',
  function ($q, $controller, $timeout, $http, $window, $translate, $scope, $filter, $routeParams, $rootScope,Restangular, ngDialog, FilteredSessionsFactory, InteractionsBulkFactory, AllLaunchedElementsBulkFactory, ProcPermissionsFactory, TreeBuilder, BPStationsFactory, SessionsFactory, BProcessesFactory, BProcessFactory, ObserversFactory, ObserverFactory, BProcessesFactory,BPSpacesFactory,BPSpaceElemsFactory, BPStationsFactory, BPStationFactory, BPLogsFactory, $location, $route) {

$scope.isEmptyLaunchesCheck = function() {
  //$rootScope.$on('cfpLoadingBar:completed', function(event, data){
  console.log("$scope.isEmptyLaunchesStart")
  console.log($scope.isEmptyLaunches);
   $scope.isEmptyLaunches = false;
   console.log(($scope.sessions));
   var AllSessions = _.flatten(_.map($scope.sessions, function(ses) { return ses.sessions }));
   console.log(AllSessions);

  if ($scope.sessions != undefined && AllSessions.length > 0) {
    var vals = _.map($scope.sessions, function(ses) {
        if (ses.sessions.length > 0) {
          $scope.isEmptyLaunches = true;
        }
        else {
          $scope.isEmptyLaunches = false;
        }
   });
    var logicSum = _.reduce(vals, function(v,z){ return v || z });
    if (logicSum != undefined) {
    $scope.isEmptyLaunches = logicSum;
    } else { $scope.isEmptyLaunches = false; }
  } else {
    //if ($scope.sessions != undefined && $scope.sessions.length === 0) {
    //  $scope.isEmptyLaunches = true;
    //} else {
      $scope.isEmptyLaunches = true//; }
  }
  console.log("$scope.isEmptyLaunches")
  console.log($scope.isEmptyLaunches)
  //});
}
$scope.sessions = [];

$scope.loadSessions = function (process_id) {

/*
process: Object
  business: 2
  id: 3
  service: 1
  state_machine_type: "base"
  title: "ttt"
  version: 1
  __proto__: Object
sessions: Array[5]
  0: Object
  peoples: Object
  percent: 0
  process: Object
  session: Object
  station: Object
  */
$scope.isEmptyLaunches = false;
//$scope.isEmptyLaunchesCheck();
$scope.lastChecked = false;



SessionsFactory.query().$promise.then(function (data2) {
    _.forEach(data2.sessions, function(session) {
      session.station.inlineLaunchShow = false;
      $scope.loadPerm(session.process);
//      return session.session.station = session.station
      return session.process.logs = BPLogsFactory.query({  BPid: session.process.id });
    });
    _.forEach(data2, function(d){ return d.process.logs = BPLogsFactory.query({  BPid: d.process.id }); })
//  if (data2.sessions === undefined || data2.sessions.length === 0) { $scope.isEmptyLaunchesCheck();$scope.lastChecked = true; }
  if (data2.length > 0) {
    if (data2[0] != undefined) {
           ProcPermissionsFactory.query({ BPid: data2[0].process.id }).$promise.then(function(qu){
                $scope.perms = qu.elemperms;

                $scope.accounts = qu.accounts;
                $scope.emps = qu.employees;
                $scope.employee_groups = qu.employee_groups;
                _.forEach($scope.employee_groups, function(gr){ return gr.group = true; });
                $scope.groups = qu.employee_groups;
                $scope.employees_groups = _.union($scope.emps,$scope.employee_groups);
                _.forEach($scope.perms, function(perm) {
                    if (perm.group != undefined) {
                      var group = _.find($scope.groups, function(group) {return group.id === perm.group});
                      if (group != undefined) {
                        perm.title = group.title;
                      } else {
                        perm.title = "";
                      }
                    }
                })

 var session_ids = _.map(data2, function(d){

    return _.map(_.filter(d.sessions, function(fd) {
      return (fd.station !== undefined) && (fd.station.finished !== true);
    }), function(dd){
        return 'ids='+dd.session.id+'&'
    })
   });




$scope.allLaunchedElemPromise = AllLaunchedElementsBulkFactory.queryAll({ids: (session_ids + '').split(',').join('') });
$scope.allLaunchedElemPromise.$promise.then(function (d) {
    $scope.allLaunchedElem = d;
    console.log("132",$scope.allLaunchedElem);
});
$scope.interactionContainerPromise = InteractionsBulkFactory.queryAll({ids: (session_ids + '').split(',').join('') });
$scope.interactionContainerPromise.$promise.then(function (d) {
    $scope.interactionContainerLaunch = d;
    console.log("137",$scope.interactionContainerLaunch);
});

/*
  Restangular.all('users').getList()  // GET: /users
    .then(function(users) {
    // returns a list of users
    $scope.user = users[0]; // first Restangular obj in list: { id: 123 }
  })
*/

    // polyfill ended
     if (process_id != undefined) {
        console.log(process_id);
        $scope.sessions = _.filter(data2, function(dat) {
          return dat.process.id === process_id

        });
     } else {
      $scope.sessions = data2;
   }
    $scope.lastChecked = true;

    $scope.treePromises = $scope.buildTree(data2, $scope.allLaunchedElemPromise)
    $scope.isEmptyLaunchesCheck();
    });
    } //else { $scope.lastChecked = true; console.log("else");$scope.isEmptyLaunchesCheck() }
  } else { $scope.lastChecked = true; $scope.isEmptyLaunches = true; }
/*
BProcessesFactory.query().$promise.then(function (proc) {
    if (proc.length > 0) {
      console.log('loadPerm');
      $scope.loadPerm(proc[0].id);
    };
    $scope.bprocesses = proc;
}); // load polyfill for accounts TODO: Change that!!!!!!
  */
});

}; // </ load session

$scope.nestedRequestScopes = [];

$scope.buildTree = function(processes, allLaunchedElemPromise) {
  var deferred = $q.defer();

   _.forEach(processes, function(d){  // entity
    return _.forEach(d.sessions, function(s) {
      return TreeBuilder.launchBuildFetch(d.process,
                                          s.session,
                                          $scope.allLaunchedElemPromise,
                                          function(success){});
      });
  });
   deferred.resolve();
   return deferred.promise;
}



$scope.reloadSessionFn = function(session_id) {
  FilteredSessionsFactory.query({id: session_id}).$promise.then(function (data2) {
    _.forEach(data2.sessions, function(session) {
      session.station.inlineLaunchShow = false;
      $scope.loadPerm(session.process);
//      return session.session.station = session.station
      return session.process.logs = BPLogsFactory.query({  BPid: session.process.id });
    });
    _.forEach(data2, function(d){ return d.process.logs = BPLogsFactory.query({  BPid: d.process.id }); })
//  if (data2.sessions == undefined || data2.sessions.length == 0) { $scope.isEmptyLaunchesCheck();$scope.lastChecked = true; }
  if (data2.length > 0) {
    if (data2[0] != undefined) {
           ProcPermissionsFactory.query({ BPid: data2[0].process.id }).$promise.then(function(qu){
                $scope.perms = qu.elemperms;

                $scope.accounts = qu.accounts;
                $scope.emps = qu.employees;
                $scope.employee_groups = qu.employee_groups;
                _.forEach($scope.employee_groups, function(gr){ return gr.group = true; });
                $scope.groups = qu.employee_groups;
                $scope.employees_groups = _.union($scope.emps,$scope.employee_groups);
                _.forEach($scope.perms, function(perm) {
                    if (perm.group != undefined) {
                      var group = _.find($scope.groups, function(group) {return group.id === perm.group});
                      if (group != undefined) {
                        perm.title = group.title;
                      } else {
                        perm.title = "";
                      }
                    }
                })
var session_ids = _.map(data2, function(d){
    return _.map(_.filter(d.sessions, function(fd) {
      return (fd.station !== undefined) && (fd.station.finished !== true);
    }), function(dd){
        return 'ids='+dd.session.id+'&'
    })
});


InteractionsBulkFactory.queryAll({ids: (session_ids + '').split(',').join('') }).$promise.then(function(updatedInteraction) {

  console.log("219",$scope.interactionContainerLaunch);
  console.log("219", updatedInteraction);

  // Update interaction container

  //$scope.interactionContainerLaunch = _.map($scope.interactionContainerLaunch, function(icon) {
  //  if (icon.session_container.sessions[0].session.id === session_id) {
  //    icon = d[0]; // Take first element(first is interaction container for updated session)
  //    console.log("icon finded",icon);
  //    return icon;
  //  } else {
  //    return icon;
  //  }
  //});

  //$scope.interactionContainerLaunch = InteractionsBulkFactory.queryAll({ids: (session_ids + '').split(',').join('') });
    $scope.updateSessionInCollection(data2[0]);
    //$scope.$broadcast('reloadSession', session_id);
_.forEach($scope.nestedRequestScopes, function(sc) {
  if (sc.session_id === session_id) {
    console.log('scope before', sc.scope.interactions);
    console.log('scope before', $scope.interactionContainerLaunch);
/*
  _.map(updatedInteraction, function(icon) {
    if (icon !== undefined && icon.session_container.sessions[0].session.id === session_id) {
      icon = d[0]; // Take first element(first is interaction container for updated session)
      console.log("icon finded",icon);
      console.log('scope after', sc.scope.interactions);
      return icon;
    //} else {
    //  return icon;
    }

  }).filter(function(n){ return n != undefined })[0]; // nested interactions must be an object, not an array.
*/  //return sc.scope.interactions = updatedInteraction;//

  }
});

  $scope.$broadcast('newInteractionsForLaunch', {session_id: session_id, updatedInteraction: updatedInteraction});
  $scope.$broadcast('reloadElementRoutine', session_id);

  $scope.lastChecked = true;
   _.forEach(data2, function(d){  // entity
    _.forEach(d.sessions, function(s) { // read session from entity
        return TreeBuilder.launchBuildFetch(d.process, s.session, $scope.allLaunchedElemPromise, function(success){});
    })
  });
  $scope.isEmptyLaunchesCheck();

            });
      });
    } //else { $scope.lastChecked = true; console.log("else");$scope.isEmptyLaunchesCheck() }
  } else { $scope.lastChecked = true; $scope.isEmptyLaunches = true; }
  });
}

$scope.updateSessionInCollection = function(session, collection) {
  console.log('updateSessionInCollection');
  console.log(session);
  console.log($scope.sessions);
  var process = session.process.id;
  var updatedSession = session.sessions[0];
  // Iterate over container for process
  $scope.sessions = _.map($scope.sessions, function(sessionContainer) {
    if (sessionContainer.process.id === process) {
  // Iterate over process session for updated session
  //sessionContainer.sessions = _.without(sessionContainer.sessions, _.find(sessionContainer.sessions, function(s) {
  //                                                                            return s.session.id === updatedSession.session.id }));
  //sessionContainer.sessions.push(updatedSession);
  _.forEach(sessionContainer.sessions, function(session) {
    if (session.session.id === updatedSession.session.id) {
      console.log('update session attr', session);
      var updated = _.reduce(session, function(obj, val, key) {
          console.log('obj[key]', obj[key]);
          console.log('updatedSession[key]', updatedSession[key]);
          obj[key] = updatedSession[key]//val;
          return obj;
      }, {});
      session.station = updatedSession.station;
      session.percent = updatedSession.percent;
      session = updated;
      return session;
    }
  })

  // Take clean collection and pass updated process to that collection
  // ???
  // PROFIT!!!
    }
    return sessionContainer;
  });
  console.log($scope.sessions);
}

$scope.reloadSession = function(session_id) {
  if ($routeParams.process != undefined) {
    $scope.loadSessions($routeParams.process);
  }
  if (session_id) {
    console.log('reload session from request controller');
    $scope.reloadSessionFn(session_id);

  } else { // Load all process
    $scope.loadSessions();
  }
};



$scope.reloadSession();
$scope.history_session_id = [];
$scope.history_entity = [];


$scope.$on('launchLocker', function(event, args) {
  console.log('launchLocker');
    console.log(event);
    console.log(args);
    $scope.reloadSession();
    // do what you want to do
});
$scope.$on('reloadSession', function(event, args) {
  console.log('launchLocker');
    console.log(event);
    console.log(args);
    $scope.reloadSession();
    // do what you want to do
});


$scope.history = function(session_id, entity) {
    $scope.historyMode = "launch";
    $scope.history_session_id = session_id;
    $scope.history_entity = entity;
    ngDialog.open({
        template: '/assets/partials/histories/launch-history.html',
        //template: '/assets/partials/popup/first-process-finished.html',
        controller: 'HistoriesCtrl',
        scope: $scope
      });
}
$scope.processHistory = function(entity) {
    $scope.historyMode = "process";
    $scope.history_session_id = undefined;
    $scope.history_entity = entity;
    ngDialog.open({
        template: '/assets/partials/histories/process-history.html',
        //template: '/assets/partials/popup/first-process-finished.html',
        controller: 'HistoriesCtrl',
        scope: $scope
      });
}
$scope.deleteSession = function(session_id) {
  SessionsFactory.delete({ session_id: session_id}).$promise.then(function (data2) {
      $scope.reloadSession();
  })
};


$scope.loadPerm = function (bpId) {
  ProcPermissionsFactory.query({ BPid: bpId }).$promise.then(function(qu){
      $scope.perms = qu.elemperms;

      $scope.accounts = qu.accounts;
      $scope.emps = qu.employees;
      $scope.employee_groups = qu.employee_groups;
      _.forEach($scope.employee_groups, function(gr){ return gr.group = true; });
      $scope.groups = qu.employee_groups;
      $scope.employees_groups = _.union($scope.emps,$scope.employee_groups);
      _.forEach($scope.perms, function(perm) {
      if (perm.group != undefined) {
        var group = _.find($scope.groups, function(group) {return group.id === perm.group});
        if (group != undefined) {
          perm.title = group.title;
        } else {
          perm.title = "";
        }
      }
      })
  });
};


$scope.accFetchSessioned = function (obj) {
  if (obj != undefined) { // it's employee

  var res = _.find($scope.accounts, function(cr){ return cr.userId === obj});
  if (res != undefined) { // it's account
                          // anonumous checking
    res.fullName != undefined ? res.tooltip = res.fullName : res.tooltip = res.email;
    if (res.avatarUrl === undefined || res.avatarUrl === "") {
      res.avatarUrl = 'data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz48IURPQ1RZUEUgc3ZnIFBVQkxJQyAiLS8vVzNDLy9EVEQgU1ZHIDEuMS8vRU4iICJodHRwOi8vd3d3LnczLm9yZy9HcmFwaGljcy9TVkcvMS4xL0RURC9zdmcxMS5kdGQiPjxzdmcgdmVyc2lvbj0iMS4xIiBpZD0iRWJlbmVfMSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIiB4bWxuczp4bGluaz0iaHR0cDovL3d3dy53My5vcmcvMTk5OS94bGluayIgeD0iMHB4IiB5PSIwcHgiIHdpZHRoPSIxNnB4IiBoZWlnaHQ9IjE2cHgiIHZpZXdCb3g9IjAgMCAxNiAxNiIgZW5hYmxlLWJhY2tncm91bmQ9Im5ldyAwIDAgMTYgMTYiIHhtbDpzcGFjZT0icHJlc2VydmUiPjx0aXRsZT5EZWZhdWx0IEF2YXRhcjwvdGl0bGU+PGRlc2M+RGVmYXVsdCBBdmF0YXIgZm9yIFdDRiAyLjA8L2Rlc2M+IDxkZWZzPjxzdHlsZSB0eXBlPSJ0ZXh0L2NzcyI+PCFbQ0RBVEFbLnN1cmZhY2UgeyBmaWxsOiAjZmZmOyB9LnNoYWRvdyB7IGZpbGw6ICNiYmI7IH1dXT48L3N0eWxlPjwvZGVmcz48cmVjdCB4PSIwIiBjbGFzcz0ic2hhZG93IiB3aWR0aD0iMTYiIGhlaWdodD0iMTYiLz48cGF0aCBjbGFzcz0ic3VyZmFjZSIgZD0iTTMuNTI4LDE2QzMuNzc2LDExLjQ5OSw1LjY4NCw4LDgsOHM0LjIyNCwzLjQ5OSw0LjQ3Myw3Ljk5OCIvPjxjaXJjbGUgY2xhc3M9InN1cmZhY2UiIGN4PSI4IiBjeT0iNiIgcj0iMy41Ii8+PC9zdmc+'
    }
    return res;
  } else if (res === undefined) {
    //if (res.avatarUrl === undefined || res.avatarUrl === "") {
      res = {};
      res.avatarUrl = 'data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz48IURPQ1RZUEUgc3ZnIFBVQkxJQyAiLS8vVzNDLy9EVEQgU1ZHIDEuMS8vRU4iICJodHRwOi8vd3d3LnczLm9yZy9HcmFwaGljcy9TVkcvMS4xL0RURC9zdmcxMS5kdGQiPjxzdmcgdmVyc2lvbj0iMS4xIiBpZD0iRWJlbmVfMSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIiB4bWxuczp4bGluaz0iaHR0cDovL3d3dy53My5vcmcvMTk5OS94bGluayIgeD0iMHB4IiB5PSIwcHgiIHdpZHRoPSIxNnB4IiBoZWlnaHQ9IjE2cHgiIHZpZXdCb3g9IjAgMCAxNiAxNiIgZW5hYmxlLWJhY2tncm91bmQ9Im5ldyAwIDAgMTYgMTYiIHhtbDpzcGFjZT0icHJlc2VydmUiPjx0aXRsZT5EZWZhdWx0IEF2YXRhcjwvdGl0bGU+PGRlc2M+RGVmYXVsdCBBdmF0YXIgZm9yIFdDRiAyLjA8L2Rlc2M+IDxkZWZzPjxzdHlsZSB0eXBlPSJ0ZXh0L2NzcyI+PCFbQ0RBVEFbLnN1cmZhY2UgeyBmaWxsOiAjZmZmOyB9LnNoYWRvdyB7IGZpbGw6ICNiYmI7IH1dXT48L3N0eWxlPjwvZGVmcz48cmVjdCB4PSIwIiBjbGFzcz0ic2hhZG93IiB3aWR0aD0iMTYiIGhlaWdodD0iMTYiLz48cGF0aCBjbGFzcz0ic3VyZmFjZSIgZD0iTTMuNTI4LDE2QzMuNzc2LDExLjQ5OSw1LjY4NCw4LDgsOHM0LjIyNCwzLjQ5OSw0LjQ3Myw3Ljk5OCIvPjxjaXJjbGUgY2xhc3M9InN1cmZhY2UiIGN4PSI4IiBjeT0iNiIgcj0iMy41Ii8+PC9zdmc+'
    //}
    return "Anonymous " + obj;
  }
  } else { // it's group
    var res = _.find($scope.groups, function(gr) { return gr.id === obj.id });
    if (res != undefined) {
      res.avatarUrl = '/assets/images/group.png'
      res.tooltip = "Group " + res.title;
      return res;
    } else { return }
  }
};
$scope.showInlineLaunch = function (session) {
  if (session.inlineLaunchShow) {
    session.inlineLaunchShow = false;
  } else {
    session.inlineLaunchShow = true;
  }
  return session;
}


$scope.isManager = function () {
  if ($scope.isManagerVal === undefined && $rootScope.manager != undefined) {
    $scope.isManagerVal = $rootScope.manager;
    return $scope.isManagerVal;
  } else {
    return $window.localStorage.manager === "true";
  }
};

$scope.isManagerVal = $scope.isManager();
$scope.isManager();

  /* callback for ng-click 'editUser': */


$scope.bubbleTooltip = function(session) {
  if (session.station != undefined) {

  if (session.station.finished) {
    return  "Finished";
  } else if (session.station.paused) {
     return "Paused";
  } else if (session.station.started) {
     return "Started";
  }
  } else {
    return "";
  }


};
$scope.bubbleClass = function(session) {
    if (session.station != undefined) {

  if (session.station.finished) {
    return  "finished";
  } else if (session.station.paused) {
     return "paused";
  } else if (session.station.started) {
     return "started";
  }
  } else { return ""; }
};

$scope.shareLaunch = function(launch, launch_id) {
  console.log(launch_id);

  $http({
      url: '/share/launch/'+launch_id,
      method: "POST",
      data: {  }
      })
      .then(function(response) {
        // success
        //$scope.stationsRefresh(); // Not for session controller
        launch.shareLink = response.data;
        //$scope.invoke_res = [response];
      },
      function(response) { // optional
        // failed
      }
      );

}

$scope.stationByProcess = function (processId) {
        var found = $filter('filter')($scope.bprocesses, {id: processId}, true);
         if (found.length) {
             return found[0];
         } else {
             '1';
         }
}
$scope.unlisted = function (session) {
  $http({
      url: '/session/' + session.session.id + '/unlisted',
      method: "POST",
      data: {  }
      })
      .then(function(response) {
        // success
        //$scope.stationsRefresh(); // Not for session controller
        $scope.reloadSession();
        $route.reload();
        //$scope.invoke_res = [response];
      },
      function(response) { // optional
        // failed
      }
      );
}
$scope.listed = function (session) {
  $http({
      url: '/session/' + session.session.id + '/listed',
      method: "POST",
      data: {  }
      })
      .then(function(response) {
        // success
        //$scope.stationsRefresh(); // Not for session controller
        $scope.reloadSession();
        $route.reload();
        //$scope.invoke_res = [response];
      },
      function(response) { // optional
        // failed
      }
      );
}

$scope.pin = function (session) {
  if (session.session.active_listed === true) {
    $scope.unlisted(session)
  }
  else {
    $scope.listed(session)
  }
}
$scope.isPinned = function (session) {
if (session.session.active_listed === true) {
      return "pinned";
  } else {
      return "";
  }
}
$scope.cancelSession = function (session) {
    $http({
      url: '/bprocess/' + session.process.id + '/session/' + session.session.id + '/halt',
      method: "POST",
      data: {  }
      })
      .then(function(response) {
        // success
        $location.path('/a#/launches/');

      },
      function(response) { // optional
        // failed
      }
      );
}

$scope.haltStation = function (stationId) {
  var bpId = $route.current.params.BPid ;
    $http({
      url: '/bprocess/' + bpId + '/station/' + stationId + '/halt',
      method: "POST",
      data: {  }
      })
      .then(function(response) {
        // success
        $scope.reloadSession();
        //$scope.stationsRefresh(); // not for session controller
        //$scope.invoke_res = [response];
      },
      function(response) { // optional
        // failed
      }
      );

  }


  $scope.onlyActive = false;
  $scope.onlyCanceled = false;
  $scope.onlyFinished = false;
  $scope.onlyListed = false;

$scope.isOnlyActive = function(session) {
    if (session.station !== undefined) {

    if ($scope.onlyActive && !$scope.onlyPaused && !$scope.onlyFinished) {
      return session.station.paused === true && session.station.state !== false;
    }
    if (!$scope.onlyActive && !$scope.onlyPaused && $scope.onlyFinished) {
      return session.station.finished === true;
    }
    if (!$scope.onlyActive && $scope.onlyCanceled) {
      return session.station.canceled === true;
    }
    else {
      return session;
    }
    } else {
      return false;
    }
}

$scope.isSessionsOnlyActive = function(sessions) {
   return _.filter(sessions, function(session){ return $scope.isOnlyActive(session) }).length < 1;
}
$scope.isOnlyListed = function(session) {
  return session;
}

$scope.date = {startDate: null, endDate: null};

}]);
});