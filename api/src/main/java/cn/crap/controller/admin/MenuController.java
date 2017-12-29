package cn.crap.controller.admin;

import cn.crap.adapter.MenuAdapter;
import cn.crap.dto.MenuDto;
import cn.crap.framework.JsonResult;
import cn.crap.framework.MyException;
import cn.crap.framework.base.BaseController;
import cn.crap.framework.interceptor.AuthPassport;
import cn.crap.model.mybatis.Menu;
import cn.crap.model.mybatis.MenuCriteria;
import cn.crap.service.mybatis.MenuService;
import cn.crap.utils.Page;
import cn.crap.utils.TableField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping
public class MenuController extends BaseController {
    @Autowired
    private MenuService menuService;

    /**
     * 根据父菜单、菜单名、菜单类型及页码获取菜单列表
     *
     * @return
     */
    @RequestMapping("/menu/list.do")
    @ResponseBody
    @AuthPassport(authority = C_AUTH_MENU)
    public JsonResult list(String type, String menuName, String parentId, @RequestParam(defaultValue = "1") Integer currentPage) {
        Page page = new Page(15, currentPage);

        MenuCriteria menuCriteria = new MenuCriteria();
        MenuCriteria.Criteria criteria = menuCriteria.createCriteria();

        if (menuName != null) {
            criteria.andMenuNameLike("%" + menuName + "%");
        }
        if (type != null) {
            criteria.andTypeEqualTo(type);
        }
        if (parentId != null) {
            criteria.andParentIdEqualTo(parentId);
        }
        menuCriteria.setOrderByClause(TableField.SORT.SEQUENCE_DESC);
        menuCriteria.setLimitStart(page.getStart());
        menuCriteria.setMaxResults(page.getSize());

        page.setAllRow(menuService.countByExample(menuCriteria));
        return new JsonResult(1, MenuAdapter.getDto(menuService.selectByExample(menuCriteria)), page);
    }

    @RequestMapping("/menu/detail.do")
    @ResponseBody
    @AuthPassport(authority = C_AUTH_MENU)
    public JsonResult detail(String id, String parentId) {
        Menu menu = new Menu();
        menu.setParentId(parentId);
        if (id != null) {
            menu = menuService.selectByPrimaryKey(id);
        }
        return new JsonResult().data(MenuAdapter.getDto(menu));
    }

    /**
     * @return
     */
    @RequestMapping("/menu/addOrUpdate.do")
    @ResponseBody
    @AuthPassport(authority = C_AUTH_MENU)
    public JsonResult addOrUpdate(@ModelAttribute MenuDto menuDto) {
        // 子菜单类型和父菜单类型一致
        Menu parentMenu = menuService.selectByPrimaryKey(menuDto.getParentId());
        if (parentMenu != null && parentMenu.getId() != null) {
            menuDto.setType(parentMenu.getType());
        }

        if (menuDto.getId() != null) {
            menuService.update(MenuAdapter.getModel(menuDto));
        } else {
            menuService.insert(MenuAdapter.getModel(menuDto));
        }
        // 清除缓存
        objectCache.del(C_CACHE_LEFT_MENU);
        return new JsonResult().data(menuDto);
    }

    @RequestMapping("/menu/delete.do")
    @ResponseBody
    @AuthPassport(authority = C_AUTH_MENU)
    public JsonResult delete(@RequestParam String id) throws MyException {
        MenuCriteria menuCriteria = new MenuCriteria();
        MenuCriteria.Criteria criteria = menuCriteria.createCriteria();
        criteria.andParentIdEqualTo(id);

        if (menuService.countByExample(menuCriteria) > 0) {
            throw new MyException("000025");
        }
        menuService.delete(id);
        // 清除缓存
        objectCache.del(C_CACHE_LEFT_MENU);
        return SUCCESS;
    }

    @RequestMapping("/back/menu/changeSequence.do")
    @ResponseBody
    @AuthPassport(authority = C_AUTH_MENU)
    public JsonResult changeSequence(@RequestParam String id, @RequestParam String changeId) {

        Menu change = menuService.selectByPrimaryKey(changeId);
        Menu model = menuService.selectByPrimaryKey(id);
        int modelSequence = model.getSequence();

        model.setSequence(change.getSequence());
        change.setSequence(modelSequence);

        menuService.update(model);
        menuService.update(change);

        // 清除缓存
        objectCache.del(C_CACHE_LEFT_MENU);
        return SUCCESS;
    }

}
