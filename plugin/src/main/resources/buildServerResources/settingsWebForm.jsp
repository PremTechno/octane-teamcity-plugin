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
            xhttp.onreadystatechange = function () {
                if (xhttp.readyState === 4) {
                    if (xhttp.status === 200) {
                        var json = JSON.parse(xhttp.responseText);
                        for (var i = 0; i < json.length; i++) {
                            addSP(i, json[i].uiLocation, json[i].username, json[i].secretPassword, json[i].impersonatedUser, json[i].identity);
                        }
                    }
                }
            };
            var parameters = "action=reload";
            xhttp.open("GET", getServletURL(), true);
            xhttp.send(parameters);
        }

        function decodeEntities(encodedString) {
            var translate_re = /&(nbsp|amp|quot|lt|gt);/g;
            var translate = {
                "nbsp":" ",
                "amp" : "&",
                "quot": "\"",
                "lt"  : "<",
                "gt"  : ">"
            };
            return encodedString.replace(translate_re, function(match, entity) {
                return translate[entity];
            }).replace(/&#(\d+);/gi, function(match, numStr) {
                var num = parseInt(numStr, 10);
                return String.fromCharCode(num);
            });
        }
    </script>


    <script>
        function getConfCount() {
            var count = document.getElementsByName("spConfigTable").length - 1;
            if (count < 0) {
                return count;
            }
            return document.getElementsByClassName("runnerFormTable")[count].id.match(/\d+/g).map(Number)[0];
        }

        function addNewSP() {
            addSP(getConfCount() + 1, '', '', '', '', '');
        }

        function addSP(index, server, clientId, clientSecret, impersonatedUser, instanceId) {
            // alert(index + ', ' + server + ', ' + clientId + ', ' + clientSecret + ', ' + instanceId + ', ' + sharedSpace);
            var spBlock = "<table name='spConfigTable' class='runnerFormTable' id='connectionsTable" + index + "' >" +
                "<tr>" +
                "<th><label for='server'>Location <span class='mandatoryAsterix' title='Mandatory field'>*</span></label></th>" +
                "<td>" +
                "<input type='text' name='server" + index + "' id='server" + index + "'   value='' class='longField'        >" +
                "<span class='error' id='errorServer" + index + "'></span>" +
                "<span style='font-size: xx-small;'>Location of the ALM Octane application</span>" +
                "</td>" +
                "</tr>" +

                "<tr>" +
                "<th><label for='username'>Client ID <span class='mandatoryAsterix' title='Mandatory field'>*</span></label></th>" +
                "<td>" +
                "<input type='text' name='username" + index + "' id='username" + index + "'   value='' class='longField'        >" +
                "<span class='error' id='errorUsername" + index + "'></span>" +
                "<span style='font-size: xx-small;'>Client ID used for logging into the ALM Octane server</span>" +
                "</td>" +
                "</tr>" +

                "<tr  >" +
                "<th><label for='password'>Client secret <span class='mandatoryAsterix' title='Mandatory field'>*</span></label></th>" +
                "<td>" +
                "<input type='password' name='password" + index + "' id='password" + index + "'   value='' class='longField'>" +
                "<span class='error' id='errorPassword" + index + "'></span>" +
                "<span style='font-size: xx-small;'>Client secret used for logging into the ALM Octane server</span>" +
                "</td>" +
                "</tr>" +

                "<tr>" +
                "<th><label for='impersonatedUser'>TeamCity user </label></th>" +
                "<td>" +
                "<input type='text' name='impersonatedUser" + index + "' id='impersonatedUser" + index + "'   value='' class='longField'        >" +
                "<span class='error' id='errorImpersonatedUser" + index + "'></span>" +
                "<span style='font-size: xx-small;'>The user to impersonate (Jobs will be executed on behalf of this user)</span>" +
                "</td>" +
                "</tr>" +

                "<tr>" +
                "<input type='hidden' name='instanceId" + index + "' id='instanceId" + index + "'   value=''>" +
                "</tr>" +

                "<tr  >" +
                "<th><label for='psw'><span class='mandatoryAsterix' title='Mandatory field'></span></label></th>" +
                "<td>" +
                "<input type='button' value='Test connection' class='btn btn_primary submitButton' id='testConnection" + index + "'  onClick='checkConnection(" + index + ")'/>" +
                "<input type='button' value='Delete' class='btn btn_primary submitButton ' id='deleteConnection" + index + "'  onClick='deleteConnection(" + index + ")'/>" +
                "</td>" +
                "</tr>" +

                "</table>";

            document.getElementById("spContainer").appendChild(htmlToElement(spBlock));

            document.getElementById("username" + index).value = clientId;
            document.getElementById("password" + index).value = clientSecret;
            document.getElementById("server" + index).value = server;
            document.getElementById("impersonatedUser" + index).value = impersonatedUser;
            document.getElementById("instanceId" + index).value = instanceId;
        }

        function htmlToElement(html) {
            var template = document.createElement('template');
            html = html.trim(); // Never return a text node of whitespace as the result
            template.innerHTML = html;
            return template.content.firstChild;
        }

        function saveParams() {
            var xhttp = new XMLHttpRequest();
            xhttp.onreadystatechange = function () {

                if (xhttp.readyState === 4) {
                    if (xhttp.status === 200) {
                        var data = JSON.parse(xhttp.responseText);
                        for (var i in data.configs) {
                            document.getElementsByClassName("runnerFormTable")[i].querySelectorAll("tbody > tr > input[name^='instanceId']")[0].value = data.configs[i];
                        }
                        message_box_div.innerHTML = decodeEntities(data.status);
                    } else {
                        message_box_div.innerHTML = "Error";
                    }
                } else {
                    message_box_div.innerHTML = "Saving...";
                }
            };

            var config = [];
            var length = document.getElementsByClassName("runnerFormTable").length;
            var i;
            for (i = 0; i < length; i++) {
                var password = document.getElementsByClassName("runnerFormTable")[i].querySelectorAll("tbody > tr > td > input[name^='password']")[0].value;
                var username = document.getElementsByClassName("runnerFormTable")[i].querySelectorAll("tbody > tr > td > input[name^='username']")[0].value;
                var server = document.getElementsByClassName("runnerFormTable")[i].querySelectorAll("tbody > tr > td > input[name^='server']")[0].value;
                var impersonatedUser = document.getElementsByClassName("runnerFormTable")[i].querySelectorAll("tbody > tr > td > input[name^='impersonatedUser']")[0].value;
                var instanceId = document.getElementsByClassName("runnerFormTable")[i].querySelectorAll("tbody > tr > input[name^='instanceId']")[0].value;

                config.push(
                    {
                        "uiLocation": server,
                        "username": username,
                        "secretPassword": password,
                        "impersonatedUser": impersonatedUser,
                        "identity": instanceId
                    }
                );
            }
            // var parameters = "action=save";
            xhttp.open("POST", getServletURL(), true);
            xhttp.setRequestHeader('Content-type', 'application/json; charset=utf-8');
            xhttp.send(JSON.stringify(config));
        }

        function getServletURL() {
            return "/octane-rest/";
        }

    </script>

    <script>
        function deleteConnection(id) {
            var toDelete = [];
            var table = document.getElementById("connectionsTable" + id);
            table.parentNode.removeChild(table);
        }



        function checkConnection(index) {
            var xhttp = new XMLHttpRequest();
            xhttp.onreadystatechange = function () {
                if (xhttp.readyState === 4) {
                    if (xhttp.status === 200) {
                        try {
                            var data = JSON.parse(xhttp.responseText);
                            message_box_div.innerHTML = decodeEntities(data.status);
                        } catch (e) {
                            message_box_div.innerHTML = 'Failed to parse response: ' + xhttp.responseText;
                        }
                    } else {
                        message_box_div.innerHTML = 'Request to TeamCity server failed with status ' + xhttp.status + ' (' + xhttp.responseText + ')';
                    }
                } else {
                    message_box_div.innerHTML = 'Waiting...';
                }
            };
            var server = encodeURIComponent(document.getElementById("server" + index).value);
            var username = encodeURIComponent(document.getElementById("username" + index).value);
            var password = encodeURIComponent(document.getElementById("password" + index).value);
            var impersonatedUser = encodeURIComponent(document.getElementById("impersonatedUser" + index).value);
            var instanceId = encodeURIComponent(document.getElementById("instanceId" + index).value);
            var parameters = "action=test&server=" + server + "&username=" + username + "&password=" + password +
                "&impersonatedUser=" + impersonatedUser + "&instanceId=" + instanceId;

            xhttp.open("POST", getServletURL(), true);
            xhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
            xhttp.send(parameters);
        }

    </script>


</head>
<body>

<div id="settingsContainer">
    <form action="/octane-rest/admin/" method="post">
        <div class="editNotificatorSettingsPage">

            <div id="spContainer">

            </div>

            <div class="saveButtonsBlock">
                <input type="button" value="Add Configuration" class="btn btn_primary submitButton"
                       onClick="addNewSP();"/>
                <input type="button" value="Save" class="btn btn_primary submitButton " onClick="saveParams()"/>
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
