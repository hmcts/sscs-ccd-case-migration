terraform {
  backend "azurerm" {
    features {
      key_vault {
        purge_soft_delete_on_destroy = false
      }
    }
  }

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 4.15.0"
    }
  }
}
