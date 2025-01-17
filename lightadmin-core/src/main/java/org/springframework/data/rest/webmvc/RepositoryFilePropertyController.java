package org.springframework.data.rest.webmvc;

import org.lightadmin.core.config.LightAdminConfiguration;
import org.lightadmin.core.config.domain.GlobalAdministrationConfiguration;
import org.lightadmin.core.rest.binary.OperationBuilder;
import org.lightadmin.core.web.util.FileResourceLoader;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.rest.core.invoke.RepositoryInvoker;
import org.springframework.data.rest.webmvc.support.BackendId;
import org.springframework.data.rest.webmvc.support.SimpleMapResource;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import static org.lightadmin.core.persistence.metamodel.PersistentPropertyType.isOfFileType;
import static org.lightadmin.core.rest.binary.OperationBuilder.operationBuilder;

@RepositoryRestController
public class RepositoryFilePropertyController extends AbstractRepositoryRestController implements ApplicationContextAware {

    private static final String BASE_MAPPING = "/{repository}/{id}/{property}";

    private ApplicationContext applicationContext;

    @Autowired
    public RepositoryFilePropertyController(PagedResourcesAssembler<Object> pagedResourcesAssembler) {
        super(pagedResourcesAssembler);
    }

    @RequestMapping(value = BASE_MAPPING + "/file", method = RequestMethod.GET)
    public void filePropertyOfEntity(RootResourceInformation repoRequest, ServletResponse response, @BackendId Serializable id, @PathVariable String property) throws Exception {
        PersistentEntity<?, ?> persistentEntity = repoRequest.getPersistentEntity();
        RepositoryInvoker invoker = repoRequest.getInvoker();

        Object domainObj = invoker.invokeFindOne(id);

        if (null == domainObj) {
            throw new ResourceNotFoundException();
        }

        PersistentProperty<?> prop = persistentEntity.getPersistentProperty(property);
        if (null == prop) {
            throw new ResourceNotFoundException();
        }

        if (isOfFileType(prop)) {
            fileResourceLoader().downloadFile(domainObj, prop, (HttpServletResponse) response);
        }
    }

    @RequestMapping(value = BASE_MAPPING + "/file", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteFileOfPropertyOfEntity(RootResourceInformation repoRequest, @BackendId Serializable id, @PathVariable String property) throws Exception {
        PersistentEntity<?, ?> persistentEntity = repoRequest.getPersistentEntity();
        RepositoryInvoker invoker = repoRequest.getInvoker();

        Object domainObj = invoker.invokeFindOne(id);

        if (null == domainObj) {
            throw new ResourceNotFoundException();
        }

        PersistentProperty<?> prop = persistentEntity.getPersistentProperty(property);
        if (null == prop) {
            throw new ResourceNotFoundException();
        }

        if (!isOfFileType(prop)) {
            return ControllerUtils.toEmptyResponse(HttpStatus.METHOD_NOT_ALLOWED);
        }

        operation().deleteOperation(domainObj).perform(prop);

        return ControllerUtils.toEmptyResponse(HttpStatus.OK);
    }

    @RequestMapping(value = BASE_MAPPING + "/file", method = {RequestMethod.POST})
    public ResponseEntity<? extends ResourceSupport> saveFilePropertyOfEntity(final ServletServerHttpRequest request) throws Exception {
        final MultipartHttpServletRequest multipartHttpServletRequest = (MultipartHttpServletRequest) request.getServletRequest();

        final Map<String, MultipartFile> fileMap = multipartHttpServletRequest.getFileMap();

        if (!fileMap.isEmpty()) {
            final Map.Entry<String, MultipartFile> fileEntry = fileMap.entrySet().iterator().next();

            return ControllerUtils.toResponseEntity(HttpStatus.OK, new HttpHeaders(), fileResource(fileEntry));
        }

        return ControllerUtils.toEmptyResponse(HttpStatus.METHOD_NOT_ALLOWED);
    }

    private Resource<?> fileResource(Map.Entry<String, MultipartFile> fileEntry) throws IOException {
        SimpleMapResource resource = new SimpleMapResource();
        resource.put("fileName", fileEntry.getValue().getOriginalFilename());
        resource.put("fileContent", fileEntry.getValue().getBytes());
        return resource;
    }

    private OperationBuilder operation() {
        return operationBuilder(globalAdministrationConfiguration(), lightAdminConfiguration());
    }

    private LightAdminConfiguration lightAdminConfiguration() {
        return applicationContext.getBean(LightAdminConfiguration.class);
    }

    private GlobalAdministrationConfiguration globalAdministrationConfiguration() {
        return applicationContext.getBean(GlobalAdministrationConfiguration.class);
    }

    private FileResourceLoader fileResourceLoader() {
        return applicationContext.getBean(FileResourceLoader.class);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}