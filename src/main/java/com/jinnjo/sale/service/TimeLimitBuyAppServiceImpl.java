package com.jinnjo.sale.service;

import com.jinnjo.sale.clients.CampaignCilent;
import com.jinnjo.sale.domain.vo.DiscountSeckillInfoVo;
import com.jinnjo.sale.domain.vo.GoodInfoVo;
import com.jinnjo.sale.domain.vo.MarketingCampaignVo;
import com.jinnjo.sale.domain.vo.SeckillGoodsVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolationException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

/**
 * @author bobo
 * @date 2019\9\10 0010 16:11
 * description:
 */
@Service
@Slf4j
public class TimeLimitBuyAppServiceImpl implements TimeLimitBuyAppService{
    private final CampaignCilent campaignCilent;
    @Autowired
    public TimeLimitBuyAppServiceImpl(CampaignCilent campaignCilent){
        this.campaignCilent = campaignCilent;
    }


    @Override
    public MarketingCampaignVo getForTop() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String date = simpleDateFormat.format(new Date());
        List<MarketingCampaignVo> marketingCampaignVoList = campaignCilent.getCampaignsByPage(date);
        if (marketingCampaignVoList.size()<=0){
            return null;
        }
        List<MarketingCampaignVo> marketingCampaignVoArrayList = new ArrayList<>();
        for (MarketingCampaignVo marketingCampaignVo:marketingCampaignVoList){
            DiscountSeckillInfoVo discountSeckillInfoVo = marketingCampaignVo.getDiscountSeckillInfo();
            if (null!=discountSeckillInfoVo){
                Date startTime = discountSeckillInfoVo.getStartSeckillTime();
                Date endTime = discountSeckillInfoVo.getEndSeckillTime();
                Date nowTime = new Date();
                if (startTime.before(nowTime)&&endTime.after(nowTime)){
                    //当前时间在活动时间段内
                    return marketingCampaignVo;
                }
                if (endTime.after(nowTime)){
                    Long start = startTime.getTime();
                    Long now = nowTime.getTime();
                    Long interval = start - now;
                    marketingCampaignVo.setInterval(interval);
                    marketingCampaignVoArrayList.add(marketingCampaignVo);
                }
            }
        }
        return marketingCampaignVoArrayList.stream().min(Comparator.comparing(MarketingCampaignVo::getInterval)).orElse(null);
    }

    @Override
    public List<MarketingCampaignVo> getForList() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String date = simpleDateFormat.format(new Date());
        return campaignCilent.getCampaignsByPage(date);
    }

    @Override
    public GoodInfoVo getGoodInfo(Long id){
        MarketingCampaignVo campaignVo = campaignCilent.getCampaignsByGoodsId(LocalDate.now().toString(), id);
        if(null == campaignVo)
            throw new ConstraintViolationException("当前商品没有设置限时购活动!", new HashSet<>());

        SeckillGoodsVo seckillGoods = campaignVo.getDiscountSeckillInfo().getSeckillGoodsList().stream().filter(seckillGoodsVo -> id.equals(seckillGoodsVo.getGoodsId())).findFirst().orElse(null);
        return new GoodInfoVo(campaignVo.getDiscountSeckillInfo(), seckillGoods);
    }
}
