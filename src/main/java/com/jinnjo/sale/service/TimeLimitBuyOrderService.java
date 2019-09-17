package com.jinnjo.sale.service;

import com.jinnjo.sale.clients.CampaignCilent;
import com.jinnjo.sale.clients.OrderClient;
import com.jinnjo.sale.domain.GoodsSkuSqr;
import com.jinnjo.sale.domain.GoodsSqr;
import com.jinnjo.sale.domain.vo.*;
import com.jinnjo.sale.repo.GoodsSqrRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.ConstraintViolationException;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashSet;

@Slf4j
@Transactional
@Service
public class TimeLimitBuyOrderService {

    private final CampaignCilent campaignCilent;

    private final OrderClient orderClient;

    private final GoodsSqrRepository goodsSqrRepository;

    @Autowired
    public TimeLimitBuyOrderService(CampaignCilent campaignCilent,
                                    OrderClient orderClient,
                                    GoodsSqrRepository goodsSqrRepository){
        this.campaignCilent = campaignCilent;
        this.orderClient = orderClient;
        this.goodsSqrRepository = goodsSqrRepository;
    }

    public OrderUnifiedVo add(OrderSubmitVo orderSubmitVo){
        //只考虑单商品购买
        OrderItemVo orderItemVo = orderSubmitVo.getVoList().get(0).getOrderItemVOList().get(0);

        MarketingCampaignVo campaignVo = campaignCilent.getCampaignsByGoodsId(LocalDate.now().toString(), orderItemVo.getGoodsId());

        if(null == campaignVo)
            return orderClient.orders(orderSubmitVo);

        //校验当前的时间是否是限时购活动时间
        if(new Date().getTime() < campaignVo.getDiscountSeckillInfo().getStartSeckillTime().getTime())
            throw new ConstraintViolationException("限时购活动时间未开始，不能购买活动商品!", new HashSet<>());

        if(new Date().getTime() > campaignVo.getDiscountSeckillInfo().getEndSeckillTime().getTime())
            throw new ConstraintViolationException("限时购活动已结束，价格发生变动，请重新下单!", new HashSet<>());


        //校验当前的商品规格是否是限时购商品规格
        SeckillGoodsVo seckillGoods = campaignVo.getDiscountSeckillInfo().getSeckillGoodsList().stream().filter(seckillGoodsVo -> orderItemVo.getGoodsId().equals(seckillGoodsVo.getGoodsId()) && orderItemVo.getSkuId().equals(seckillGoodsVo.getGoodsSpecId()))
                .findFirst()
                .orElse(null);

        if(null == seckillGoods)
            return orderClient.orders(orderSubmitVo);

        orderItemVo.setDiscountPrice(seckillGoods.getSeckillPrice()); //商品折扣价设置为秒杀价格

        GoodsSqr goodsSqr = goodsSqrRepository.findByIdAndStatusIsNot(orderItemVo.getGoodsId(), Short.valueOf("-1"));

        orderItemVo.setType(goodsSqr.getType());
        orderItemVo.setBuyType(goodsSqr.getBuyType());
        orderItemVo.setExpenses(goodsSqr.getExpenses());
        orderItemVo.setExpensesTaxation(goodsSqr.getExpensesTaxation());
        orderItemVo.setIsCertification(Integer.parseInt(goodsSqr.getIsCertification()));
        orderItemVo.setPlatformCharge(goodsSqr.getPlatformCharge());
        orderItemVo.setTsfGoodsId(String.valueOf(goodsSqr.getTsfGoodsId()));
        orderItemVo.setDistributionType(goodsSqr.getDistributionType());
        orderItemVo.setIsShareGoods(goodsSqr.getIsShareGoods());
        orderItemVo.setIsIntegralGoods(goodsSqr.getIsIntegralGoods());
        orderItemVo.setBonus(goodsSqr.getBonus());
        orderItemVo.setCommission(goodsSqr.getCommission());
        orderItemVo.setTitle(goodsSqr.getTitle());
        orderItemVo.setIcon(goodsSqr.getIcon());
        orderItemVo.setSource(goodsSqr.getSource());


        GoodsSkuSqr goodsSkuSqr = goodsSqr.getSkuInfos().stream().filter(skuInfo -> skuInfo.getId().equals(orderItemVo.getSkuId())).findFirst().orElse(null);
        if(null == goodsSkuSqr)
            throw new ConstraintViolationException(orderItemVo.getSkuId() + "规格不存在!", new HashSet<>());

        orderItemVo.setPrice(goodsSkuSqr.getPrice());
        orderItemVo.setSpStrVal(goodsSkuSqr.getSpStrVal());
        orderItemVo.setStock(goodsSkuSqr.getStock());


        return orderClient.limitTimeOrder(orderSubmitVo);
    }
}