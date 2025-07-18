import * as pulumi from "@pulumi/pulumi";
import { buildAwsStack } from "./modules/aws";
import { buildAzureStack } from "./modules/azure";
import { buildGcpStack } from "./modules/gcp";

const cfg = new pulumi.Config("haven");
const provider = process.env.CLOUD_PROVIDER ?? "aws";

let outputs: any;
if (provider === "aws") outputs = buildAwsStack(cfg);
else if (provider === "azure") outputs = buildAzureStack(cfg);
else outputs = buildGcpStack(cfg);

export const dbEndpoint = outputs.dbEndpoint;
export const k8sClusterName = outputs.clusterName;