/*
 *     2017 EntIT Software LLC, a Micro Focus company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.hp.octane.plugins.jetbrains.teamcity.actions;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.connectivity.HttpMethod;
import com.hp.octane.integrations.dto.connectivity.OctaneResultAbridged;
import com.hp.octane.integrations.dto.connectivity.OctaneTaskAbridged;
import com.hp.octane.integrations.services.tasking.TasksProcessor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by lazara on 07/02/2016.
 */

public class GenericOctaneActionsController implements Controller {
    private static final DTOFactory dtoFactory = DTOFactory.getInstance();

    @Override
    public ModelAndView handleRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {

        HttpMethod method = null;
        if ("post".equals(req.getMethod().toLowerCase())) {
            method = HttpMethod.POST;
        } else if ("get".equals(req.getMethod().toLowerCase())) {
            method = HttpMethod.GET;
        } else if ("put".equals(req.getMethod().toLowerCase())) {
            method = HttpMethod.PUT;
        } else if ("delete".equals(req.getMethod().toLowerCase())) {
            method = HttpMethod.DELETE;
        }
        if (method != null) {
            OctaneTaskAbridged octaneTaskAbridged = dtoFactory.newDTO(OctaneTaskAbridged.class)
                    .setId(UUID.randomUUID().toString())
                    .setMethod(method)
                    .setUrl(req.getRequestURI())
                    .setBody("");
            OctaneSDK.getClients().forEach(client -> {

                TasksProcessor taskProcessor = client.getTasksProcessor();
                OctaneResultAbridged result = taskProcessor.execute(octaneTaskAbridged);
                res.setStatus(result.getStatus());
                try {
                    res.getWriter().write(result.getBody());
                } catch (IOException e) {
                    res.setStatus(501);
                    e.printStackTrace();
                }
            });
        } else {
            res.setStatus(501);
        }
        return null;
    }
}
