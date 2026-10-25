import { Supplier } from '../../suppliers/models/supplier';
import { DEFAULT_WEIGHTS, KEY_FROM_CRITERION } from '../helper/rfq-page.helper';
import {
  RfqDetailDto,
  RfqItem,
  RfqRecord,
  RfqScoreWeights,
  RfqSummaryDto,
  RfqSupplier,
  SupplierQuote,
} from '../model/rfq.models';

export class RfqPageMapper {
  static fromSummary(dto: RfqSummaryDto): RfqRecord {
    return {
      id: dto.id,
      code: dto.rfqCode,
      title: dto.title,
      procurementTicketId: dto.procurementTicketId,
      prCode: dto.procurementTicketCode,
      department: dto.department ?? '',
      createdBy: dto.createdBy,
      creatorName: dto.creatorName || dto.createdBy,
      createdAt: dto.createdAt,
      deadline: dto.deadline,
      validityDays: 0,
      deliveryLocation: '',
      deliveryWindow: '',
      estimatedValue: dto.estimatedValue,
      status: dto.status,
      invitedSupplierIds: [],
      invitedSupplierCount: dto.invitedSupplierCount,
      invitedSuppliers: [],
      respondedSupplierCount: dto.respondedSupplierCount,
      items: [],
      quotes: [],
      history: [],
      weights: { ...DEFAULT_WEIGHTS },
    };
  }

  static fromDetail(dto: RfqDetailDto): RfqRecord {
    const items: RfqItem[] = (dto.items ?? [])
      .filter((item) => item.active)
      .map((item) => ({
        id: item.id,
        procurementItemId: item.procurementItemId,
        sku: item.sku ?? '',
        name: item.itemName,
        category: item.category,
        quantity: item.quantity,
        unit: item.unit ?? '',
        budgetUnitPrice: item.budgetUnitPrice,
        active: item.active,
      }));
    const weights = { ...DEFAULT_WEIGHTS };
    for (const criterion of dto.scoreCriteria ?? [])
      weights[KEY_FROM_CRITERION[criterion.criterion]] = criterion.weightPercent;
    const quotes: SupplierQuote[] = (dto.quoteRounds ?? [])
      .filter((round) => round.status === 'SUBMITTED')
      .map((round) => {
        const lineItems = new Map(round.items.map((line) => [line.rfqItemId, line]));
        const scores: RfqScoreWeights = {
          price: 0,
          technical: 0,
          delivery: 0,
          payment: 0,
          reputation: 0,
        };
        for (const score of round.scores ?? [])
          scores[KEY_FROM_CRITERION[score.criterion]] = score.score;
        return {
          id: round.id,
          supplierId: round.supplierId,
          supplierCode: round.supplierCode,
          supplierName: round.supplierName,
          round: round.roundNumber,
          submittedAt: round.submittedAt,
          validUntil: round.validUntil ?? '',
          validityDays: round.validUntil
            ? Math.max(
                0,
                Math.ceil(
                  (new Date(`${round.validUntil}T00:00:00`).getTime() - Date.now()) / 86_400_000,
                ),
              )
            : 0,
          unitPrices: Object.fromEntries(
            items.map((item) => {
              const line = lineItems.get(item.id);
              return [item.id, line?.quoted ? (line.unitPrice ?? null) : null];
            }),
          ),
          shipping: round.shippingCost,
          taxAmount: round.taxAmount,
          otherCost: round.otherCost,
          deliveryDays: round.deliveryDays ?? 0,
          paymentTerms: round.paymentTerms ?? '',
          warranty: round.warranty ?? '',
          technicalStatus: round.technicalStatus ?? 'PARTIAL',
          score: scores,
          totalScore: round.totalScore,
          rank: round.rank,
        };
      });
    const itemWinners: Record<string, string> = {};
    const itemReasons: Record<string, string> = {};
    for (const award of dto.itemAwards ?? []) {
      itemWinners[award.rfqItemId] = award.supplierId;
      itemReasons[award.rfqItemId] = award.reason ?? '';
    }
    return {
      ...this.fromSummary(dto),
      validityDays: dto.quoteValidityDays,
      deliveryLocation: dto.deliveryLocation ?? '',
      deliveryWindow: dto.deliveryWindow ?? '',
      invitedSuppliers: dto.invitedSuppliers ?? [],
      invitedSupplierCount: (dto.invitedSuppliers ?? []).filter(
        (invite) => invite.status !== 'REVOKED',
      ).length,
      invitedSupplierIds: (dto.invitedSuppliers ?? [])
        .filter((invite) => invite.status !== 'REVOKED')
        .map((invite) => invite.supplierId),
      respondedSupplierCount: (dto.invitedSuppliers ?? []).filter(
        (invite) => invite.status === 'RESPONDED',
      ).length,
      items,
      quotes,
      weights,
      history: (dto.history ?? []).map((entry) => ({
        actor: entry.actorUsername,
        action: entry.action,
        at: entry.createdAt,
        detail: entry.details
          ? String(entry.details['reason'] ?? JSON.stringify(entry.details))
          : undefined,
      })),
      reviewedAt: dto.reviewedAt,
      reviewedBy: dto.reviewedBy,
      chosenSupplierId: dto.selectedSupplierId,
      chosenSupplierName: dto.selectedSupplierName,
      selectionReason: dto.selectionReason,
      decisionReason: dto.reviewReason,
      itemWinners,
      itemReasons,
      exceptionReason: dto.exceptionReason,
    };
  }

  static fromSupplier(supplier: Supplier): RfqSupplier {
    return {
      id: supplier.id,
      code: supplier.code,
      name: supplier.name,
      status: supplier.status,
      blacklisted: supplier.status === 'BLACKLISTED',
    };
  }
}
