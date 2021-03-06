package com.xuecheng.manage_course.service.impl;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.domain.cms.response.CmsPostPageResult;
import com.xuecheng.framework.domain.course.*;
import com.xuecheng.framework.domain.course.ext.CourseInfo;
import com.xuecheng.framework.domain.course.ext.CourseView;
import com.xuecheng.framework.domain.course.ext.TeachplanNode;
import com.xuecheng.framework.domain.course.request.CourseListRequest;
import com.xuecheng.framework.domain.course.response.AddCourseResult;
import com.xuecheng.framework.domain.course.response.CourseCode;
import com.xuecheng.framework.domain.course.response.CoursePublishResult;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_course.client.CmsPageClient;
import com.xuecheng.manage_course.dao.*;
import com.xuecheng.manage_course.service.CourseService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class CourseServiceimpl implements CourseService{
    @Autowired
    CourseBaseRepository courseBaseRepository;
    @Autowired
    private TeachplanRepository teachplanRepository;
    @Autowired
    private TeachplanMapper teachplanMapper;
    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    CourseMarketRepository  courseMarketRepository;

    @Autowired
    CoursePicRepository coursePicRepository;

    @Autowired
    CmsPageClient cmsPageClient;

    @Autowired
    CoursePubRepositoy coursePubRepositoy;

    @Autowired
    TeachplanMediaRepository teachplanMediaRepository;

    @Autowired
    TeachplanMediaPubRepository teachplanMediaPubRepository;
    @Value("${course???publish.dataUrlPre}")
    private String publish_dataUrlPre;
    @Value("${course???publish.pagePhysicalPath}")
    private String publish_page_physicalpath;
    @Value("${course???publish.pageWebPath}")
    private String publish_page_webpath;
    @Value("${course???publish.siteId}")
    private String publish_siteId;
    @Value("${course???publish.templateId}")
    private String publish_templateId;
    @Value("${course???publish.previewUrl}")
    private String previewUrl;
    @Override
    public TeachplanNode findTeachplanList(String courseid) {
        return teachplanMapper.selectList(courseid);
    }

    /**
     * ??????????????????
     * @param teachplan  ???????????????
     * @return
     */
    @Override
    @Transactional
    public ResponseResult addTeachplan(Teachplan teachplan) {
        //????????????????????????
        if(teachplan==null || StringUtils.isEmpty(teachplan.getCourseid())||StringUtils.isEmpty(teachplan.getPname())){
            ExceptionCast.cast(CommonCode.INVALITADE);
        }
        //????????????id
        String courseid = teachplan.getCourseid();
        //??????????????????id
        String parentid = teachplan.getParentid();
        if(StringUtils.isEmpty(parentid)){
            //??????????????????????????????
            parentid = findTeachplan(courseid);
        }
        Optional<Teachplan> optional = teachplanRepository.findById(parentid);
        Teachplan teachplan1 = optional.get();
        String parentid1 = teachplan1.getGrade();

        Teachplan teachplanNew=new Teachplan();
        //???????????????????????????????????????
        BeanUtils.copyProperties(teachplan,teachplanNew);
        teachplanNew.setCourseid(courseid);
        teachplanNew.setParentid(parentid);
        if(parentid1.equals("1")){
            teachplanNew.setGrade("2");
        }else{
            teachplanNew.setGrade("3");
        }
        teachplanRepository.save(teachplanNew);
        return new ResponseResult(CommonCode.SUCCESS);
    }



    /**
     * ????????????id ??????  ?????????????????????????????????
     * @param courseid  ??????id
     * @return
     */
    private String findTeachplan(String courseid){
        //????????????id??????
        Optional<CourseBase> optional = courseBaseRepository.findById(courseid);
        if(!optional.isPresent()){
            return null;
        }
        //??????????????????
        CourseBase courseBase = optional.get();
        //?????????????????????
        List<Teachplan> teachplanList = teachplanRepository.findByCourseidAndAndParentid(courseid, "0");
       //?????????????????? ??????????????????
        if(teachplanList==null||teachplanList.size()<=0){
            Teachplan teachplan=new Teachplan();
            //???????????????id
            teachplan.setParentid("0");
            //????????????
            teachplan.setGrade("1");
            //??????????????????
            teachplan.setPname(courseBase.getName());
            //????????????id
            teachplan.setCourseid(courseid);
            //??????????????????
            teachplan.setStatus("0");
            teachplanRepository.save(teachplan);
            return teachplan.getId();
        }
        return teachplanList.get(0).getId();
    }



    /**
     *
     * @param page ?????????
     * @param size ??????????????????
     * @param courseListRequest  ????????????
     * @return
     */
    @Override
    public QueryResponseResult findCourseList(String company_id,int page, int size, CourseListRequest courseListRequest) {
       if(courseListRequest==null){
           courseListRequest=new CourseListRequest();
       }
       if(!StringUtils.isEmpty(company_id)){
           courseListRequest.setCompanyId(company_id);
       }

       if(page<=0){
           page=0;
       }
       if(size<=0){
           size=20;
       }
       //??????????????????   ?????????????????????
        PageHelper.startPage(page,size);
        //??????????????????
        Page<CourseInfo> courseList = courseMapper.findCourseList(courseListRequest);
        //????????????  ????????????
        List<CourseInfo> result = courseList.getResult();
        //??????????????????
        long total = courseList.getTotal();
        //?????????????????????
        QueryResult<CourseInfo> courseIncfoQueryResult = new QueryResult<CourseInfo>();
        courseIncfoQueryResult.setList(result);
        courseIncfoQueryResult.setTotal(total);
        return new QueryResponseResult(CommonCode.SUCCESS, courseIncfoQueryResult);
    }

    @Override
    @Transactional
    public AddCourseResult addCourseBase(CourseBase courseBase) {
        //??????????????????????????????
        System.out.println(courseBase);
        courseBase.setStatus("202001");
        courseBaseRepository.save(courseBase);
        return new AddCourseResult(CommonCode.SUCCESS,courseBase.getId());
    }

    @Override
    public CourseBase getCoursebaseById(String courseid) {
        Optional<CourseBase> optional = courseBaseRepository.findById(courseid);
        if(optional.isPresent()){
            return optional.get();
        }
        return null;
    }

    @Override
    @Transactional
    public ResponseResult updateCoursebase(String id, CourseBase courseBase) {
        CourseBase one = this.getCoursebaseById(id);
        if(one == null){
//????????????..
            ExceptionCast.cast(CourseCode.COURSE_PUBLISH_COURSEIDISNULL);
        }
//??????????????????
        one.setName(courseBase.getName());
        one.setMt(courseBase.getMt());
        one.setSt(courseBase.getSt());
        one.setGrade(courseBase.getGrade());
        one.setStudymodel(courseBase.getStudymodel());
        one.setUsers(courseBase.getUsers());
        one.setDescription(courseBase.getDescription());
        CourseBase save = courseBaseRepository.save(one);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    @Override
    public CourseMarket getCourseMarketById(String courseid) {
        Optional<CourseMarket> optional = courseMarketRepository.findById(courseid);
        if(!optional.isPresent()){
            return optional.get();
        }
        return null;
    }

    @Override
    @Transactional
    public CourseMarket updateCourseMarket(String id, CourseMarket courseMarket) {
        CourseMarket one = this.getCourseMarketById(id);
        if(one!=null){
            one.setCharge(courseMarket.getCharge());
            one.setStartTime(courseMarket.getStartTime());//??????????????????????????????
            one.setEndTime(courseMarket.getEndTime());//??????????????????????????????
            one.setPrice(courseMarket.getPrice());
            one.setQq(courseMarket.getQq());
            one.setValid(courseMarket.getValid());
            courseMarketRepository.save(one);
        }else{
//????????????????????????
            one = new CourseMarket();
            BeanUtils.copyProperties(courseMarket, one);
//????????????id
            one.setId(id);
            courseMarketRepository.save(one);
        }
        return one;
    }

    /**
     * ??????????????????????????????????????????
     * @param courseId
     * @param pic
     * @return
     */
    @Override
    @Transactional
    public ResponseResult addCoursepic(String courseId, String pic) {
        /**
         * ?????????????????????
         */
        CoursePic coursePic=null;
        Optional<CoursePic> optional = coursePicRepository.findById(courseId);
        if(optional.isPresent()){
           coursePic=optional.get();
        }
        if(coursePic==null){
            coursePic=new CoursePic();
        }
        coursePic.setCourseid(courseId);
        coursePic.setPic(pic);
        coursePicRepository.save(coursePic);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    @Override
    public CoursePic findCoursePic(String courseid) {
        Optional<CoursePic> optional =
                coursePicRepository.findById(courseid);
        if(optional.isPresent()){
            CoursePic coursePic = optional.get();
            return coursePic;
        }
        return null;
    }

    @Override
    @Transactional
    public ResponseResult deleteCoursePic(String courseId) {
        long deleteByCourseid = coursePicRepository.deleteByCourseid(courseId);
        if(deleteByCourseid>0){
            return new ResponseResult(CommonCode.SUCCESS);
        }
        return new ResponseResult(CommonCode.FAIL);
    }

    /**
     * ?????????????????????  ??????
     * @param id
     * @return
     */
    @Override
    public CourseView getCourseView(String id) {
        CourseView courseView=new CourseView();
        /*??????????????????*/
        Optional<CourseBase> courseBase = courseBaseRepository.findById(id);
        if(courseBase.isPresent()){
            CourseBase courseBase1 = courseBase.get();
            courseView.setCourseBase(courseBase1);
        }
        /*????????????*/
        Optional<CoursePic> optionalCoursePic = coursePicRepository.findById(id);
        if(optionalCoursePic.isPresent()){
            CoursePic coursePic = optionalCoursePic.get();
            courseView.setCoursePic(coursePic);
        }
        /*??????????????????*/
        Optional<CourseMarket> optionalCourseMarket = courseMarketRepository.findById(id);
        if(optionalCourseMarket.isPresent()){
            CourseMarket courseMarket = optionalCourseMarket.get();
            courseView.setCourseMarket(courseMarket);
        }
        /*??????????????????*/
        TeachplanNode teachplanNode = teachplanMapper.selectList(id);
        courseView.setTeachplanNode(teachplanNode);
        return courseView;
    }

    //??????id????????????????????????
    public CourseBase findCourseBaseById(String courseId){
        Optional<CourseBase> baseOptional = courseBaseRepository.findById(courseId);
        if(baseOptional.isPresent()){
            CourseBase courseBase = baseOptional.get();
            return courseBase;
        }
        ExceptionCast.cast(CourseCode.COURSE_GET_NOTEXISTS);
        return null;
    }
            /**
     * ????????????
     * @param id
     * @return
     */
    @Override
    public CoursePublishResult preview(String id) {
        //????????????id????????????
        CourseBase baseById = findCourseBaseById(id);
        //??????cms ????????????  //????????????fine
        //??????cmspage ?????????
        CmsPage cmsPage=new CmsPage();
        cmsPage.setSiteId(publish_siteId);
        cmsPage.setDataUrl(publish_dataUrlPre+id);
        cmsPage.setPageName(id+".html");
        cmsPage.setPageAliase(baseById.getName());
        cmsPage.setPagePhysicalPath(publish_page_physicalpath);
        cmsPage.setPageWebPath(publish_page_webpath);
        cmsPage.setTemplateId(publish_templateId);
        //????????????cmspage ??????????????????
        CmsPageResult save = cmsPageClient.save(cmsPage);
        //?????????????????????url
        if(!save.isSuccess()){
            return new CoursePublishResult(CommonCode.FAIL,null);
        }
        //???????????? ?????????????????????
        CmsPage cmsPage1 = save.getCmsPage();
        String pageId = cmsPage1.getPageId();
        //?????????????????????url
        String previewUr=previewUrl+pageId;
        return new CoursePublishResult(CommonCode.SUCCESS,previewUr);
    }

    /**
     * ????????????
     * @param id
     * @return
     */
    @Override
    @Transactional
    public CoursePublishResult publih(String id) {
        //????????????id????????????
        CourseBase baseById = findCourseBaseById(id);
        //??????cms ?????????????????????????????????????????????????????????
        CmsPage cmsPage=new CmsPage();
        cmsPage.setSiteId(publish_siteId);
        cmsPage.setDataUrl(publish_dataUrlPre+id);
        cmsPage.setPageName(id+".html");
        cmsPage.setPageAliase(baseById.getName());
        cmsPage.setPagePhysicalPath(publish_page_physicalpath);
        cmsPage.setPageWebPath(publish_page_webpath);
        cmsPage.setTemplateId(publish_templateId);
        CmsPostPageResult pageResult = cmsPageClient.postPageQuick(cmsPage);
        if(!pageResult.isSuccess()){
            return new CoursePublishResult(CommonCode.FAIL,null);
        }
        //???????????????????????????????????????
        CourseBase courseBase = saveCoursePubState(id);
        if(courseBase==null){
            return new CoursePublishResult(CommonCode.FAIL,null);
        }
        //????????????????????????
        //???????????????coursePub??????
        CoursePub coursePub = createCoursePub(id);
        //???coursePub????????????????????????
        saveCoursePub(id,coursePub);
        //?????????????????????
        //...

        //???????????????????????????  ???????????????
        String pageUrl = pageResult.getPageUrl();
        //????????????id??????????????????
        saveTeachplanMediaPub(id);
        //???teachplanMedia????????? ??????
        return new CoursePublishResult(CommonCode.SUCCESS,pageUrl);
    }

    /**
     * ????????????id ??????????????????
     * @param courseId
     */
    private void saveTeachplanMediaPub(String courseId){
        //????????????????????????
        List<TeachplanMedia> teachplanMediaList = teachplanMediaRepository.findByCourseId(courseId);
//?????????????????????????????????????????????
        teachplanMediaPubRepository.deleteByCourseId(courseId);
        List<TeachplanMediaPub> teachplanMediaPubList = new ArrayList<>();
        for(TeachplanMedia teachplanMedia:teachplanMediaList){
            TeachplanMediaPub teachplanMediaPub =new TeachplanMediaPub();
            BeanUtils.copyProperties(teachplanMedia,teachplanMediaPub);
            teachplanMediaPub.setTimestamp(new Date());
            teachplanMediaPubList.add(teachplanMediaPub);
        }
        teachplanMediaPubRepository.saveAll(teachplanMediaPubList);
    }


    //???coursePub????????????????????????
    private CoursePub saveCoursePub(String id,CoursePub coursePub){

        CoursePub coursePubNew = null;
        //????????????id??????coursePub
        Optional<CoursePub> coursePubOptional = coursePubRepositoy.findById(id);
        if(coursePubOptional.isPresent()){
            coursePubNew = coursePubOptional.get();
        }else{
            coursePubNew = new CoursePub();
        }

        //???coursePub???????????????????????????coursePubNew???
        BeanUtils.copyProperties(coursePub,coursePubNew);
        coursePubNew.setId(id);
        //?????????,???logstach??????
        coursePubNew.setTimestamp(new Date());
        //????????????
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
        String date = simpleDateFormat.format(new Date());
        coursePubNew.setPubTime(date);
        coursePubRepositoy.save(coursePubNew);

        return coursePubNew;
    }
    //??????coursePub??????
    private CoursePub createCoursePub(String id){
        CoursePub coursePub = new CoursePub();
        //????????????id??????course_base
        Optional<CourseBase> baseOptional = courseBaseRepository.findById(id);
        if(baseOptional.isPresent()){
            CourseBase courseBase = baseOptional.get();
            //???courseBase???????????????CoursePub???
            BeanUtils.copyProperties(courseBase,coursePub);
        }
        //??????????????????
        Optional<CoursePic> picOptional = coursePicRepository.findById(id);
        if(picOptional.isPresent()){
            CoursePic coursePic = picOptional.get();
            BeanUtils.copyProperties(coursePic, coursePub);
        }

        //??????????????????
        Optional<CourseMarket> marketOptional = courseMarketRepository.findById(id);
        if(marketOptional.isPresent()){
            CourseMarket courseMarket = marketOptional.get();
            BeanUtils.copyProperties(courseMarket, coursePub);
        }

        //??????????????????
        TeachplanNode teachplanNode = teachplanMapper.selectList(id);
        String jsonString = JSON.toJSONString(teachplanNode);
        //?????????????????????json???????????? course_pub???
        coursePub.setTeachplan(jsonString);
        return coursePub;

    }

    /**
     * ????????????????????????
     * @return
     */
    private CourseBase saveCoursePubState(String courseId){
        CourseBase courseBase = findCourseBaseById(courseId);
        courseBase.setStatus("202002");
        CourseBase save = courseBaseRepository.save(courseBase);
        return save;
    }
    /**
     * ????????????????????????????????????
     * @param teachplanMedia
     * @return
     */
    @Override
    public ResponseResult saveMedia(TeachplanMedia teachplanMedia) {
        if(teachplanMedia==null||StringUtils.isEmpty(teachplanMedia.getTeachplanId())){
            ExceptionCast.cast(CommonCode.INVALITADE);
        }
        //???????????????????????????????????????
        String teachplanId = teachplanMedia.getTeachplanId();
        //??????????????????
        Optional<Teachplan> optional = teachplanRepository.findById(teachplanId);
        if(!optional.isPresent()){
            ExceptionCast.cast(CommonCode.INVALITADE);
        }
        //?????????????????????
        Teachplan teachplan = optional.get();
        //????????????
        String grade = teachplan.getGrade();
        if(StringUtils.isEmpty(grade) || !grade.equals("3")){
            //?????????????????????????????????
            ExceptionCast.cast(CourseCode.COURSE_MEDIA_TEACHPLAN_GRADEERROR);
        }
        //??????
        Optional<TeachplanMedia> mediaOptional = teachplanMediaRepository.findById(teachplanId);
        TeachplanMedia teachplanMedia1=null;
        if(mediaOptional.isPresent()){
            teachplanMedia1 = mediaOptional.get();
        }else {
            teachplanMedia1=new TeachplanMedia();
        }
        // teachplanMedia1 ??????????????????
        teachplanMedia1.setCourseId(teachplan.getCourseid());
        //??????id
        teachplanMedia1.setMediaId(teachplanMedia.getMediaId());
        //?????????????????????
        teachplanMedia1.setMediaFileOriginalName(teachplanMedia.getMediaFileOriginalName());
        //url
        teachplanMedia1.setMediaUrl(teachplanMedia.getMediaUrl());
        //???????????????id
        teachplanMedia1.setTeachplanId(teachplanId);
        TeachplanMedia save = teachplanMediaRepository.save(teachplanMedia1);
        return new ResponseResult(CommonCode.SUCCESS);
    }
}
