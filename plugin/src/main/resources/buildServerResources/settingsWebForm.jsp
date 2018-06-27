<%--
  ~     2017 EntIT Software LLC, a Micro Focus company, L.P.
  ~     Licensed under the Apache License, Version 2.0 (the "License");
  ~     you may not use this file except in compliance with the License.
  ~     You may obtain a copy of the License at
  ~
  ~       http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~     Unless required by applicable law or agreed to in writing, software
  ~     distributed under the License is distributed on an "AS IS" BASIS,
  ~     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~     See the License for the specific language governing permissions and
  ~     limitations under the License.
  ~
  --%>
<jsp:useBean id="conf" class="com.hp.octane.plugins.jetbrains.teamcity.OctaneTeamCityPlugin"/>

<html>
<head>
    <script>
        function loadDoc() {
            var xhttp = new XMLHttpRequest();
            xhttp.onreadystatechange = function() {
                if (xhttp.readyState == 4 && xhttp.status == 200) {
                    var obj = JSON.parse(xhttp.responseText);
                    document.getElementById("server").value  = obj.uiLocation;
                    document.getElementById("username1").value  = obj.username;
                    document.getElementById("password1").value  = obj.secretPassword;
                }
            };
            var parameters ="action=reload";
            xhttp.open("GET", getServletURL(), true);
            xhttp.send(parameters);
        }
    </script>


    <script>
        function saveParams() {
            var xhttp = new XMLHttpRequest();
            xhttp.onreadystatechange = function() {

                if(xhttp.readyState == 4) {
                    if (xhttp.status == 200)
                        message_box_div.innerHTML = xhttp.responseText;
                    else
                        message_box_div.innerHTML = "Error"
                }else{
                    message_box_div.innerHTML = "Saving...";
                }
            };
            var server= encodeURIComponent(document.getElementById("server").value);
            var username = encodeURIComponent(document.getElementById("username1").value);
            var password =encodeURIComponent(document.getElementById("password1").value);
            var parameters = "action=save"+"&server="+server+"&username1="+username+"&password1="+password;

            xhttp.open("POST", getServletURL() , true);
            xhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
            xhttp.send(parameters);
        }

        function getServletURL(){
            var rootURL = "${conf.getServerUrl()}";
            if(!rootURL){
                return "/octane-rest/";
            }
            return rootURL + "/octane-rest/";
        }

    </script>




    <script>
        function checkConnection() {
            var xhttp = new XMLHttpRequest();
            xhttp.onreadystatechange = function() {
                if(xhttp.readyState == 4) {
                    if (xhttp.status == 200)
                        message_box_div.innerHTML = xhttp.responseText;
                    else
                        message_box_div.innerHTML = "Error"
                }else{
                    message_box_div.innerHTML = "Waiting...";
                }
            };
            var server= encodeURIComponent(document.getElementById("server").value);
            var username = encodeURIComponent(document.getElementById("username1").value);
            var password =encodeURIComponent(document.getElementById("password1").value);
            var parameters = "action=test&server="+server+"&username1="+username+"&password1="+password;

            xhttp.open("POST", getServletURL(), true);
            xhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded")
            xhttp.send(parameters)
        }

    </script>



</head>
<body>

<div id="settingsContainer">
    <form action="/octane-rest/admin/" method="post" >
        <div class="editNotificatorSettingsPage">
            <table class="runnerFormTable">
                <tr>
                    <th><label for="server">Location <span class="mandatoryAsterix" title="Mandatory field">*</span></label></th>
                    <td>
                        <input type="text" name="server" id="server"   value="" class="longField"        >
                        <span class="error" id="errorServer"></span>
                        <span style="font-size: xx-small;">Location of the ALM Octane application</span>
                    </td>
                </tr>

                <tr>
                    <th><label for="username1">Client ID <span class="mandatoryAsterix" title="Mandatory field">*</span></label></th>
                    <td>
                        <input type="text" name="username1" id="username1"   value="" class="longField"        >
                        <span class="error" id="errorUsername1"></span>
                        <span style="font-size: xx-small;">Client ID used for logging into the ALM Octane server</span>
                    </td>
                </tr>

                <tr>
                    <th><label for="password1">Client secret <span class="mandatoryAsterix" title="Mandatory field">*</span></label></th>
                    <td>
                        <input type="password" name="password1" id="password1"   value="" class="longField"        >
                        <span class="error" id="errorPassword"></span>
                        <span style="font-size: xx-small;">Client secret used for logging into the ALM Octane server</span>
                    </td>
                </tr>

            </table>

            <div class="saveButtonsBlock">


                <input type="button" value="Save" class="btn btn_primary submitButton "   onClick="saveParams()"  />

                <input type="button" value="Test connection" class="btn btn_primary submitButton " id="testConnection"  onClick="checkConnection()"  />

            </div>
        </div>

    </form>
</div>
<div id="message_box_div" class="message_box_div">
</div>
<script>
    loadDoc()
</script>
</body>
</html>
